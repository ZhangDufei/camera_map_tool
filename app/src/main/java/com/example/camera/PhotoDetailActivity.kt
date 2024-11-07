package com.example.camera

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File

@Suppress("DEPRECATION")
class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var photoImageView: ImageView
    private lateinit var coordinatesTextView: TextView
    private lateinit var pitchTextView: TextView
    private lateinit var azimuthTextView: TextView
    private lateinit var buildingNameTextView: TextView
    private lateinit var buildingCoordinatesTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        // 设置 Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 初始化视图
        photoImageView = findViewById(R.id.photo_image_view)
        coordinatesTextView = findViewById(R.id.coordinates_text_view)
        pitchTextView = findViewById(R.id.pitch_text_view)
        azimuthTextView = findViewById(R.id.azimuth_text_view)
        buildingNameTextView = findViewById(R.id.building_name_text_view)
        buildingCoordinatesTextView = findViewById(R.id.building_coordinates_text_view)

        // 设置返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "照片详情"

        // 获取传递过来的 PhotoData
        val photoData: PhotoData? = intent.getSerializableExtra("photo_data") as? PhotoData

        // 绑定数据
        photoData?.let { bind(it) } ?: showError()
    }

    private fun bind(photoData: PhotoData) {
        // 加载图片
        photoImageView.setImageURI(Uri.fromFile(File(photoData.photoPath)))
        coordinatesTextView.text = photoData.coordinates
        pitchTextView.text = "俯仰角度：${photoData.pitch}"
        azimuthTextView.text = "方位角：${photoData.azimuth}"
        buildingNameTextView.text = photoData.buildingName ?: "信息科学与技术学院"
        buildingCoordinatesTextView.text = photoData.buildingCoordinates ?: "43.8234690, 125.4197007"
    }

    private fun showError() {
        // 显示未找到数据的提示
        coordinatesTextView.text = "未找到照片数据"
        pitchTextView.text = "未找到俯仰角度"
        azimuthTextView.text = "未找到方位角"
        buildingNameTextView.text = "未找到建筑物名称"
        buildingCoordinatesTextView.text = "未找到建筑物坐标"
    }

    // 处理返回按钮点击事件
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}