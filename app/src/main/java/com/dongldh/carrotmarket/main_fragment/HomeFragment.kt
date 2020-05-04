package com.dongldh.carrotmarket.main_fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dongldh.carrotmarket.R
import com.dongldh.carrotmarket.database.DataItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.item_uploaded_item.view.*

class HomeFragment: Fragment() {
    val auth: FirebaseAuth? = FirebaseAuth.getInstance()
    val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.main_recycler.layoutManager = LinearLayoutManager(activity)
        view.main_recycler.adapter = MainAdapter()
        return view
    }

    class MainViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val photo = view.featured_image
        val title = view.title_text
        val location = view.location_text
        val price = view.price_text
    }

    inner class MainAdapter: RecyclerView.Adapter<MainViewHolder>() {
        var itemList = mutableListOf<DataItem>()

        init {
            fireStore.collection("UsedItems").orderBy("timeStamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null) { return@addSnapshotListener }
                itemList.clear()

                for(snapshot in querySnapshot!!.documents) {
                    val item = snapshot?.toObject(DataItem::class.java)
                    itemList.add(item!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return MainViewHolder(layoutInflater.inflate(R.layout.item_uploaded_item, parent, false))
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
            val item = itemList[position]

            // 게시글 올린 시간과 현재 시간과의 차이를 문자열로 바꾸어줌
            var time = ""
            when(val timePassed = System.currentTimeMillis() - item.timeStamp) {
                in 0 until 60 * 1000 -> time = "방금 전"
                in 60 * 1000 until 60 * 60 * 1000 -> time = "${timePassed / 60000}분 전"
                in 60 * 60 * 1000 until 24 * 60 * 60 * 1000 -> time = "${timePassed / 3600000}시간 전"
                else -> time = "${timePassed / 24 * 3600000}일 전"
            }

            // 저장된 사진이 없다면 기본 이미지를 등록, 있다면 사진 모음의 첫 사진을 등록
            if(item.photos.isNullOrEmpty()) holder.photo.setImageResource(R.mipmap.ic_launcher)
            else holder.photo.setImageURI(Uri.parse(item.photos[0]))

            holder.title.text = item.title
            holder.location.text = "${item.location} · $time"
            holder.price.text = "${String.format("%,d", item.price)}원"
        }
    }
}