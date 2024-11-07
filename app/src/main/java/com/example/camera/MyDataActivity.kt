package com.example.camera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MyDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var photoDataAdapter: PhotoDataAdapter
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_data)

        // 设置 Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 显示返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "我的数据"

        // 初始化数据库助手
        databaseHelper = DatabaseHelper(this)

        // 设置 RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 加载数据
        loadPhotoData()

        // 设置底部导航栏
        setupBottomNavigation()
    }

    private fun loadPhotoData() {
        val photoDataList = databaseHelper.getAllData()
        photoDataAdapter = PhotoDataAdapter(photoDataList) { photoData ->
            val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                putExtra("photo_data", photoData) // 使用相同的键名
            }
            startActivity(intent)
        }
        recyclerView.adapter = photoDataAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 跳转到主页面
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.navigation_my_data -> {
                    // 当前页面
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}