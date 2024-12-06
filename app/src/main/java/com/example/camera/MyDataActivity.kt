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
        val photoDataList = databaseHelper.getAllData() // 获取所有数据
        photoDataAdapter = PhotoDataAdapter(photoDataList.toMutableList(), { photoData ->
            // 点击项时的逻辑
            val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                putExtra("photo_data", photoData)
            }
            startActivity(intent)
        }, { photoData ->
            // 处理删除操作
            deletePhotoData(photoData) // 调用删除方法
        })
        recyclerView.adapter = photoDataAdapter
    }


    private fun deletePhotoData(photoData: PhotoData) {
        val isDeleted = databaseHelper.deletePhotoData(photoData) // 通过 ID 删除数据
        if (isDeleted) {
            // 删除成功，更新 RecyclerView
            loadPhotoData() // 重新加载数据
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_my_data
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_my_data -> true
                R.id.navigation_home -> {
                    // 跳转到主页面
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
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