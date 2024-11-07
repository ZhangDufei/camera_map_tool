package com.example.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mapView: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var orientation: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 osmdroid 配置
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        // 初始化 MapView
        setupMapView()

        // 检查和请求定位权限
        checkLocationPermissions()

        // 初始化传感器
        initSensors()

        // 设置加号按钮事件
        findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            startActivity(Intent(this, TaskActivity::class.java))
        }

        // 设置底部导航栏
        setupBottomNavigation()
    }

    private fun setupMapView() {
        mapView = findViewById<MapView>(R.id.map).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun enableMyLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        mapController.setCenter(geoPoint)

        // 在地图上添加用户位置标记
        mapView.overlays.clear() // 清除之前的标记
        mapView.overlays.add(createMarker(geoPoint, "我的位置"))
        mapView.invalidate() // 刷新地图
    }

    private fun createMarker(position: GeoPoint, title: String): Marker {
        return Marker(mapView).apply {
            this.position = position
            this.title = title
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ORIENTATION) {
            orientation = event.values[0] // 获取指南针方向
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 处理传感器精度变化
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            // 权限被拒绝，提示用户启用权限
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume() // 必须调用以恢复地图状态
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // 必须调用以保存地图状态
        locationManager.removeUpdates(this) // 停止位置更新
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 当前页面
                    true
                }
                R.id.navigation_my_data -> {
                    // 跳转到“我的数据”页面
                    val intent = Intent(this, MyDataActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}