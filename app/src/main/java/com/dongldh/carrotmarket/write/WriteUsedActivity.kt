package com.dongldh.carrotmarket.write

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dongldh.carrotmarket.CircleProgressDialog
import com.dongldh.carrotmarket.R
import com.dongldh.carrotmarket.database.*
import com.dongldh.carrotmarket.dialog.WriteUsedCategoryDialog
import com.dongldh.carrotmarket.setting.SettingLocationActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_write_used.*
import kotlinx.android.synthetic.main.activity_write_used.back_image
import kotlinx.android.synthetic.main.activity_write_used.image_count_layout
import kotlinx.android.synthetic.main.activity_write_used.next_text
import kotlinx.android.synthetic.main.item_write_photo_item.view.*
import java.text.SimpleDateFormat
import java.util.*


class WriteUsedActivity : AppCompatActivity(), View.OnClickListener {
    var auth: FirebaseAuth? = null
    var fireStore: FirebaseFirestore? = null
    var storage: FirebaseStorage? = null

    var location: String? = null    // 등록되는 지역
    var locationNear: String? = null    // 몇 칸 떨어진 거리까지 허용 가능한가에 대한 변수
    var photoUriList = mutableListOf<Uri>()   // 받아온 사진 Uri
    var firebasePhotoUriList = arrayListOf<String>() // 사진 Uri를 string으로 바꿈 - 파이어베이스는 String형의 list만 허용 가능하므로

    var isPossibleSuggestion = true // 가격 제안 가능?
    var counter = 0 // 업로드 성공, 혹은 실패 처리 된 이미지의 수를 카운팅.

    var progressDialog: CircleProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_used)

        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        Permissions(applicationContext).permissionStorage()     // 내가 만든 Permissions 클래스

        countPhotos() // 등록된 사진의 갯수 수정
        // 아래에 있는 지역과 인접정보를 변경해준다.
        location = intent.getStringExtra("location")
        locationNear = intent.getStringExtra("locationNear")
        write_used_near_location_text.text = getString(R.string.write_used_near_location_text)
            .replace("xx", location!!)
            .replace("yy", locationNear!!)

        image_count_layout.setOnClickListener(this)
        write_used_category_layout.setOnClickListener(this)
        write_used_price_suggest_check_layout.setOnClickListener(this)
        write_used_near_location_layout.setOnClickListener(this)
        back_image.setOnClickListener(this)
        next_text.setOnClickListener(this)

        // 금액 입력 하면 WON 표시에 불 들어오게끔 ^^
        write_used_price_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s!!.isEmpty()) write_used_price_text.setTextColor(Color.parseColor("#cfcfcf"))
                else write_used_price_text.setTextColor(Color.BLACK)
            }
        })
    }

    // 상품의 예시 이미지를 imageView에 띄워주는 어댑터클래스
    class WriteViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val image = view.item_item_image
        val delete = view.item_delete_text
    }
    
    inner class WriteAdapter(val list: MutableList<Uri>): RecyclerView.Adapter<WriteViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WriteViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return WriteViewHolder(layoutInflater.inflate(R.layout.item_write_photo_item, parent, false))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: WriteViewHolder, position: Int) {
            val uri = photoUriList[position]

            holder.image.setImageURI(uri)
            holder.delete.setOnClickListener {
                photoUriList.removeAt(position)
                countPhotos() // 등록된 사진의 갯수 수정
                write_used_recycler.adapter = WriteAdapter(photoUriList)    // recyclerView 다시 업데이트 해야지!
            }
        }

    }

    override fun onClick(v: View?) {
        when(v) {
            // 이미지 삽입
            image_count_layout -> {
                if(photoUriList.size == 10) Toast.makeText(this, "사진을 더 이상 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
                else {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(intent, PICK_IMAGE_FROM_ALBUM)
                }
            }

            // category 설정 가능
            // Dialog 생성자로 textView를 집어 넣었기 때문에, 해당 클래스 안에서 text의 조작이 가능하게끔 설정
            write_used_category_layout -> {
                val dialog = WriteUsedCategoryDialog(write_used_category_text)
                dialog.show(supportFragmentManager, "dialog_event")
            }

            // check를 누를 때 마다 이미지 변경 및 글자 색 변경
            // 그리고, data도 글과 관련된 정보 데이터도 바꿔 줘야 되는거 알지?
            write_used_price_suggest_check_layout -> {
                if(isPossibleSuggestion) {
                    isPossibleSuggestion = false
                    write_used_price_suggest_check_image.setImageResource(R.drawable.ic_unchecked)
                    write_used_price_suggest_check.setTextColor(Color.parseColor("#B6B6B6"))
                } else {
                    isPossibleSuggestion = true
                    write_used_price_suggest_check_image.setImageResource(R.drawable.ic_checked)
                    write_used_price_suggest_check.setTextColor(Color.BLACK)
                }
            }

            write_used_near_location_layout -> {
                // 다음 액티비티에서 firebase로 정보를 받아올 수 있음에도 불구하고
                // 계속 이렇게 intent로 값을 넘겨주는 이유는? -> 딜레이 현상 없이 바로바로 뷰에 값이 저장되도록 하기 위해서
                val intent = Intent(this, SettingLocationActivity::class.java)
                intent.putExtra("location", location)
                intent.putExtra("locationNear", locationNear)
                intent.putExtra("requestCode", "WriteUsedActivity")
                startActivityForResult(intent, FROM_WRITE_TO_SETTING_LOCATION)
            }

            back_image -> finish()
            next_text -> uploadImages()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            // 갤러리에서 돌아온 뒤의 처리
            PICK_IMAGE_FROM_ALBUM -> {
                if(resultCode == Activity.RESULT_OK) {
                    val photoUri = data?.data!!
                    photoUriList.add(photoUri)      // UriList에 받아온 Uri값을 추가해준다.
                    countPhotos() // 등록된 사진의 갯수 수정

                    val layoutManager = LinearLayoutManager(this)
                    layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                    write_used_recycler.layoutManager = layoutManager
                    write_used_recycler.adapter = WriteAdapter(photoUriList)
                }
            }

            // 돌아오면 맨 아래 지역과 떨어진 거리정보 텍스트 변경된다
            FROM_WRITE_TO_SETTING_LOCATION -> {
                if(resultCode == Activity.RESULT_OK) {
                    locationNear = data?.getStringExtra("locationNear")
                    write_used_near_location_text.text = getString(R.string.write_used_near_location_text)
                        .replace("xx", location!!)
                        .replace("yy", locationNear!!)
                }
            }
        }
    }

    // 올라와있는 사진의 수를 센 뒤, 텍스트뷰를 재 설정해준다.
    fun countPhotos() {
        val count = photoUriList.size
        image_count_text.text = getString(R.string.write_used_image_count_text).replace("xx", count.toString())
    }

    // 이미지를 storage에 저장하는 작업 + 저장이 완료 됐다면 saveImageToFireStore 메서드 호출
    fun uploadImages() {
        when {
            write_used_title_input.text.isEmpty() -> Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
            write_used_category_text.text.toString() == "카테고리" -> Toast.makeText(this, "카테고리를 선택해주세요", Toast.LENGTH_SHORT).show()
            write_used_content_input.text.length < 15 -> Toast.makeText(this, "내용이 너무 짧습니다", Toast.LENGTH_SHORT).show()
            write_used_price_input.text.toString().toInt() > 2000000000 -> Toast.makeText(this, "20억 이하의 가격을 입력해주세요", Toast.LENGTH_SHORT).show()

            else -> {
                progressDialog = CircleProgressDialog(this)
                val uid = auth?.currentUser!!.uid

                // 이미지가 등록되어 있다면 이미지 포함하여 저장. 그렇지 않으면(else) 정보만 저장
                if(photoUriList.size != 0) {
                    val storageReference = storage?.reference
                    for(uri in photoUriList) {
                        val timestamp = System.currentTimeMillis()
                        val imageFileName = "IMAGE_${uid}_${timestamp}.png"

                        // storage에 이미지 저장
                        storageReference?.child("itemImages")?.child(imageFileName)?.putFile(uri)?.addOnCompleteListener { task ->
                            if(task.isSuccessful) {
                                // 성공했을 경우, storage에 저장된 이미지를 다운 받을 수 있게 하는 url을 가져옴. 그 값을 리스트에 저장
                                storageReference.child("itemImages").child(imageFileName).downloadUrl.addOnCompleteListener { task2 ->
                                    counter++
                                    if(task2.isSuccessful) {
                                        firebasePhotoUriList.add(task2.result.toString())
                                    } else {
                                        progressDialog!!.dismiss()
                                        // 만약 이미지는 저장이 됐지만 서버 오류등으로 이미지 url을 가져오지 못했다면, 이미지 삭제 및 토스트 메세지 띄움
                                        storageReference.child("itemImages").child(imageFileName).delete()
                                        Toast.makeText(this@WriteUsedActivity, "일부 사진을 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    if(counter == photoUriList.size) {
                                        saveImageToFireStore()
                                    }
                                }
                            } else {
                                progressDialog!!.dismiss()
                                Toast.makeText(this@WriteUsedActivity, "일부 사진을 업로드할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                counter++
                            }
                        }?.addOnFailureListener {
                            progressDialog!!.dismiss()
                            Toast.makeText(this@WriteUsedActivity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else saveImageToFireStore()
            }
        }
    }

    // 글의 정보를 데이터베이스에 저장
    fun saveImageToFireStore() {
        val uid = auth?.currentUser!!.uid
        fireStore!!.collection("users").document(uid).get().addOnSuccessListener {
            val userName = it["userName"].toString()
            val type = 1
            val title = write_used_title_input.text.toString()
            val category = write_used_category_text.text.toString()
            val location = location!!
            val content = write_used_content_input.text.toString()
            val price = write_used_price_input.text.toString()
            val isPossibleSuggestion = isPossibleSuggestion

            val item = DataItem(
                userName,
                type,
                title,
                category,
                location,
                content,
                if (price == "") null else price.toInt(),
                photos = firebasePhotoUriList,
                possibleSuggestion = isPossibleSuggestion
            )

            fireStore!!.collection("UsedItems").document().set(item)
                .addOnSuccessListener {
                    Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    progressDialog!!.dismiss()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "게시글 등록에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    progressDialog!!.dismiss()
                    finish()
                }
        }.addOnFailureListener {
            progressDialog!!.dismiss()
            finish()
            Toast.makeText(this, "유저 정보에 접근을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
