package com.example.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var databaseHelper: DatabaseHelper // 使用你的数据库帮助类

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 osmdroid 配置
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        // 初始化 MapView
        setupMapView()

        // 初始化数据库帮助类
        databaseHelper = DatabaseHelper(this)

        // 检查和请求定位权限
        checkLocationPermissions()

        // 显示数据库中的坐标数据
        displayCoordinatesFromDatabase()

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

    private fun displayCoordinatesFromDatabase() {
        val photoDataList = databaseHelper.getAllData()
        val geoPoints = photoDataList.map {
            GeoPoint(
                it.coordinates.split("：")[1].split(",")[0].toDouble(),
                it.coordinates.split("：")[1].split(",")[1].toDouble()
            )
        }
        showCoordinatesOnMap(geoPoints)
    }

    private fun showCoordinatesOnMap(geoPoints: List<GeoPoint>) {
        for (geoPoint in geoPoints) {
            val marker = createMarker(geoPoint, "坐标：${geoPoint.latitude}, ${geoPoint.longitude}")
            mapView.overlays.add(marker)
        }

        // 聚焦到第一个坐标
        if (geoPoints.isNotEmpty()) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(geoPoints[0])
        }
    }

    private fun createMarker(position: GeoPoint, title: String): Marker {
        return Marker(mapView).apply {
            this.position = position
            this.title = title
        }
    }

    override fun onLocationChanged(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        mapView.controller.setCenter(geoPoint)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_my_data -> {
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