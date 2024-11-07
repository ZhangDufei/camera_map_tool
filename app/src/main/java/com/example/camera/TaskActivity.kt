package com.example.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.bumptech.glide.Glide
import java.io.File
import java.io.Serializable
import kotlin.math.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class PhotoData(
    val id: Long,
    val photoPath: String,
    val coordinates: String,
    val pitch: String,
    val azimuth: String?,
    val buildingName: String?,
    val buildingCoordinates: String?
) : Serializable

class TaskActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var angleTextView: TextView
    private lateinit var directionTextView: TextView
    private lateinit var coordinatesTextView: TextView
    private lateinit var captureButton: Button
    private lateinit var saveButton: Button
    private lateinit var previewView: PreviewView

    private var imageCapture: ImageCapture? = null
    private var isCameraActive: Boolean = false

    private var azimuth: Float = 0f
    private var pitch: Float = 0f
    private var currentLocation: String = "经纬坐标：未获取"
    private var savedImageFilePath: String? = null
    private var buildingName: String? = null
    private var buildingCoordinates: String? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometerValues = FloatArray(3)
    private var magneticFieldValues = FloatArray(3)
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var databaseHelper: DatabaseHelper

    // 用于存储建筑物信息
    private val buildings = mutableListOf<Building>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // 初始化视图
        angleTextView = findViewById(R.id.angle_text_view)
        directionTextView = findViewById(R.id.direction_text_view)
        coordinatesTextView = findViewById(R.id.coordinates_text_view)
        captureButton = findViewById(R.id.capture_button)
        saveButton = findViewById(R.id.save_button)
        previewView = findViewById(R.id.preview_view)

        // 初始化传感器管理器和位置服务
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseHelper = DatabaseHelper(this)

        // 检查权限并设置相机
        checkPermissions()
        setupCamera()

        // 拍照按钮点击事件
        captureButton.setOnClickListener {
            if (isCameraActive) {
                Log.d("TaskActivity", "Camera is active. Photo can be updated.")
                takePhoto()  // 拍照
            } else {
                takePhoto()
            }
        }

        // 保存数据按钮点击事件
        saveButton.setOnClickListener {
            saveData()  // 保存数据
        }

        fetchCurrentLocation()
        saveButton.visibility = View.GONE  // 初始隐藏保存按钮
    }

    override fun onResume() {
        super.onResume()
        registerSensors()
    }

    override fun onPause() {
        super.onPause()
        unregisterSensors()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                isCameraActive = true
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val file = createFile()
        val metadata = ImageCapture.Metadata()

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).setMetadata(metadata).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    savedImageFilePath = file.absolutePath
                    captureButton.text = "更新照片"
                    angleTextView.text = "$pitch"
                    coordinatesTextView.text = currentLocation

                    calculateBuildingData()

                    // 显示保存按钮
                    saveButton.visibility = View.VISIBLE

                    // 停止相机预览
                    stopCameraPreview()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun stopCameraPreview() {
        // 停止相机的逻辑（可根据实际情况调整）
        imageCapture = null  // 清除当前的相机绑定
    }

    private fun calculateBuildingData() {
        // 用户经纬度
        val userCoordinates = coordinatesTextView.text.toString()
        val userLatLng = userCoordinates.split("：")[1].split(",")
        val userLatitude = userLatLng[0].toDouble()
        val userLongitude = userLatLng[1].toDouble()

        // 射线参数
        val distance = 200.0  // 射线长度，单位米
        val buildingHeight = 10.0  // 假设建筑物高度为10米

        // 计算射线终点
        val rayEnd = calculate3DRayPoint(userLatitude, userLongitude, distance)

        // 获取建筑物数据并处理
        fetchBuildingData(userLatitude, userLongitude) {
            // 判断射线与建筑物的相交
            for (building in buildings) {
                if (rayBuildingIntersection(userLatitude, userLongitude, rayEnd, building)) {
                    buildingName = building.name
                    buildingCoordinates = "${building.latitude}, ${building.longitude}"
                    break
                }
            }
        }
    }

    private fun calculate3DRayPoint(latitude: Double, longitude: Double, distance: Double): Triple<Double, Double, Double> {
        // 计算射线终点坐标
        val pitchRadians = Math.toRadians(pitch.toDouble())
        val horizontalDistance = distance * cos(pitchRadians)
        val verticalDistance = distance * sin(pitchRadians)

        // 使用 Haversine 公式计算终点
        val destination = haversineDestination(latitude, longitude, horizontalDistance, azimuth)
        val endAltitude = verticalDistance // 这里假设海拔为0

        return Triple(destination.first, destination.second, endAltitude)
    }

    private fun haversineDestination(latitude: Double, longitude: Double, distance: Double, azimuth: Float): Pair<Double, Double> {
        // 将方位角转换为弧度
        val azimuthRadians = Math.toRadians(azimuth.toDouble())

        val radius = 6371000.0 // 地球半径，单位为米
        val delta = distance / radius // 相对地球半径的距离

        val destinationLatitude = asin(sin(Math.toRadians(latitude)) * cos(delta) +
                cos(Math.toRadians(latitude)) * sin(delta) * cos(azimuthRadians))
        val destinationLongitude = Math.toRadians(longitude) + atan2(sin(azimuthRadians) * sin(delta) * cos(Math.toRadians(latitude)),
            cos(delta) - sin(Math.toRadians(latitude)) * sin(destinationLatitude))

        return Pair(Math.toDegrees(destinationLatitude), Math.toDegrees(destinationLongitude))
    }

    private fun fetchBuildingData(latitude: Double, longitude: Double, callback: () -> Unit) {
        val overpassUrl = "http://overpass-api.de/api/interpreter"
        val query = """
    [out:json];
    (
      way["building"](around:100, $latitude, $longitude);
      relation["building"](around:100, $latitude, $longitude);
    );
    out body;
    >;
    out skel qt;
    """.trimIndent()

        val request = Request.Builder()
            .url("$overpassUrl?data=$query")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // 处理请求失败的情况
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val responseBody = it.body?.string()
                    responseBody?.let { body ->
                        parseBuildingData(body)
                        callback() // 在成功解析数据后调用回调
                    }
                }
            }
        })
    }

    private fun parseBuildingData(jsonData: String) {
        buildings.clear()
        val jsonObject = JSONObject(jsonData)
        val elements = jsonObject.getJSONArray("elements")

        for (i in 0 until elements.length()) {
            val element = elements.getJSONObject(i)

            // 获取建筑物的经纬度和高度信息
            val buildingName = element.optString("tags").let { tags ->
                JSONObject(tags).optString("name", "Unnamed Building")
            }
            val latitude = element.getJSONObject("latlon").getDouble("lat")
            val longitude = element.getJSONObject("latlon").getDouble("lon")
            val height = element.optString("tags").let { tags ->
                JSONObject(tags).optDouble("height", 10.0) // 默认高度为10.0米
            }

            buildings.add(Building(buildingName, latitude, longitude, height))
        }
    }

    private fun rayBuildingIntersection(userLat: Double, userLng: Double, rayEnd: Triple<Double, Double, Double>, building: Building): Boolean {
        // 建筑物的经纬度和高度
        val buildingLat = building.latitude
        val buildingLng = building.longitude
        val buildingHeight = building.height

        // 计算与建筑物的水平距离
        val distanceToBuilding = FloatArray(1)
        Location.distanceBetween(userLat, userLng, buildingLat, buildingLng, distanceToBuilding)

        // 判断俯仰角与射线的高度
        // val rayHeight = rayEnd.third  // 射线的高度
        // val isHeightValid = rayHeight >= buildingHeight

        // 计算方位角
        val bearingToBuilding = Math.toDegrees(Math.atan2(
            buildingLng - userLng,
            buildingLat - userLat
        ))

        // 计算用户的方位角
        val userAzimuth = azimuth  // 从传感器获取的用户方位角

        // 计算方位角的差值
        val bearingDifference = Math.abs(bearingToBuilding - userAzimuth)
        val isBearingValid = bearingDifference <= 45  // 允许的方位角范围，例如±45度

        // 判断距离是否在有效范围内
        val isWithinDistance = distanceToBuilding[0] <= 100  // 假设有效距离为50米

        // 返回是否相交
        return isWithinDistance && isBearingValid
    }

    private fun createFile(): File {
        val mediaDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(mediaDir, "${System.currentTimeMillis()}.jpg")
    }

    private fun saveData() {
        savedImageFilePath?.let { photoPath ->
            val photoData = PhotoData(
                id = System.currentTimeMillis(),
                photoPath = photoPath,
                coordinates = currentLocation,
                pitch = pitch.toString(),
                azimuth = azimuth.toString(),
                buildingName = buildingName,
                buildingCoordinates = buildingCoordinates
            )
            databaseHelper.insertData(photoData)

            // 这里可以选择跳转到 "我的数据" 页面
            val intent = Intent(this, MyDataActivity::class.java)
            startActivity(intent)
        }
    }
    private fun fetchCurrentLocation() {
        // 获取用户当前位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = "经纬坐标：${it.latitude}, ${it.longitude}"
                    coordinatesTextView.text = currentLocation
                }
            }
        }
    }

    private fun registerSensors() {
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerValues = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> magneticFieldValues = event.values
        }
        updateOrientation()
    }

    private fun updateOrientation() {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticFieldValues)) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

            angleTextView.text = "俯仰角度：${pitch}"
            directionTextView.text = "方向：${azimuth}"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    data class Building(val name: String, val latitude: Double, val longitude: Double, val height: Double)
}