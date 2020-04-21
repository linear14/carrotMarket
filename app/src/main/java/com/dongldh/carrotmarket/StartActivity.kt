package com.dongldh.carrotmarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // ImageView 안의 gif가 움직일 수 있도록 설정하자 (Glide 라이브러리 이용)
        Glide.with(this).load(R.drawable.rabbit).centerCrop().into(rabbit_image)
    }
}