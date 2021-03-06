package com.dongldh.carrotmarket.transaction

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.dongldh.carrotmarket.R
import com.dongldh.carrotmarket.database.DataUser
import com.dongldh.carrotmarket.dialog.DetailMoreDialog
import com.dongldh.carrotmarket.nestedFragmentState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_show_item_detail.*
import kotlinx.android.synthetic.main.fragment_show_item_detail.view.*

class DetailFragment: Fragment(), View.OnClickListener {
    lateinit var photos: ArrayList<String>
    val auth = FirebaseAuth.getInstance()
    val fireStore = FirebaseFirestore.getInstance()
    lateinit var user: DataUser

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_show_item_detail, container, false)

        photos = arguments?.getStringArrayList("photos")!!
        val userName = arguments?.getString("userName")
        val location = arguments?.getString("location")
        val title = arguments?.getString("title")
        val category = arguments?.getString("category")
        val time = arguments?.getString("time")
        val content = arguments?.getString("content")
        val type = arguments?.getInt("type")
        val price = arguments?.getInt("price")

        view.viewPager.adapter = ViewPagerAdapter()
        
        // viewPager의 위치가 변할 때 호출되는 리스너
        view.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) { circleIndicator.selectDot(position) }
        })
        
        // 원에 대한 갯수 설정 및 on_off 이미지 설정
        view.circleIndicator.createDotPanel(photos.size, R.drawable.indicator_dot_off, R.drawable.indicator_dot_on, 0)

        // 파이어베이스 document 조건 걸어서 모두 가져오기
        fireStore.collection("users")
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener {
                for(document in it) {
                    user = document.toObject(DataUser::class.java)
                    view.detail_profile_name.text = user.userName
                    view.detail_profile_location.text = location
                    if(user.profileImage == "default") {
                        view.detail_profile_image.setImageResource(R.drawable.ic_profile_default)
                    } else {
                        Glide.with(activity?.applicationContext!!)
                            .load(Uri.parse(user.profileImage))
                            .into(view.detail_profile_image)
                    }
                }
            }

        view.detail_item_info_title_text.text = title
        view.detail_toolbar_title.text = title
        view.detail_item_info_category_time_text.text = getString(R.string.detail_item_info_category_time_text)
            .replace("xx", category?:"category null 오류 - DetailFragment")
            .replace("yy", time?:"time null 오류 - DetailFragment")
        view.detail_item_info_content_text.text = content
        view.detail_other_text.text = getString(R.string.detail_other_text).replace("xx", userName?: "userName null 오류 - DetailFragment")

        if(type == 1) {
            if (price == 0) {
                view.detail_price_text.text = "무료나눔"
                view.detail_possible_suggestion_text.visibility = View.GONE
            } else {
                view.detail_price_text.text = getString(R.string.detail_price_text).replace("xx", String.format("%,d", price))
                view.detail_possible_suggestion_text.text = if (arguments?.getBoolean("possibleSuggestion", true)!!) "가격제안 가능" else "가격제안 불가"
            }
        } else {
            if (price == 0) {
                view.detail_price_text.text = "가격없음"
            } else {
                view.detail_price_text.text = getString(R.string.detail_price_text).replace("xx", String.format("%,d", price))
            }
            view.detail_possible_suggestion_text.visibility = View.GONE
        }

        view.detail_back_image.setOnClickListener(this)
        view.detail_more_image.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View?) {
        when(v) {
            detail_back_image -> {
                nestedFragmentState = false
                activity?.findViewById<View>(R.id.detail_content)?.visibility = View.GONE
                activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
            }

            detail_more_image -> {
                val dialogFragment = DetailMoreDialog()
                val bundle = Bundle()
                bundle.putString("nowUserName", arguments?.getString("nowUserName"))
                bundle.putString("uploadUserName", arguments?.getString("userName"))
                dialogFragment.arguments = bundle
                dialogFragment.show(activity?.supportFragmentManager!!, "dialog_fragment")
            }
        }
    }


    // 이미지 슬라이더를 만들기 위한 뷰 페이저 어댑터
    inner class ViewPagerAdapter: PagerAdapter() {
        private var layoutInflater: LayoutInflater? = null


        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getCount(): Int {
            return photos.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            layoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater!!.inflate(R.layout.viewpager_detail_image, null)
            val image = view.findViewById<ImageView>(R.id.detail_viewpager_imageview)

            Glide.with(context!!)
                .load(Uri.parse(photos[position]))
                .into(image)

            val viewPager = container as ViewPager
            viewPager.addView(view, 0)

            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val viewPager = container as ViewPager
            val view = `object` as View
            viewPager.removeView(view)
        }
    }
}
