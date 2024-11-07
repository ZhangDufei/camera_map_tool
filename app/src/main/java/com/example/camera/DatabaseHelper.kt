package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "photo_data.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "photos"
        private const val COLUMN_ID = "id"
        private const val COLUMN_PHOTO_PATH = "image_path"
        private const val COLUMN_COORDINATES = "location"
        private const val COLUMN_AZIMUTH = "azimuth"
        private const val COLUMN_PITCH = "pitch"
        private const val COLUMN_BUILDING_NAME = "building_name"
        private const val COLUMN_BUILDING_COORDINATES = "building_coordinates"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PHOTO_PATH TEXT,
                $COLUMN_COORDINATES TEXT,
                $COLUMN_AZIMUTH REAL,
                $COLUMN_PITCH REAL,
                $COLUMN_BUILDING_NAME TEXT,
                $COLUMN_BUILDING_COORDINATES TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(photoData: PhotoData): Long { // 接收一个 PhotoData 对象
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_PHOTO_PATH, photoData.photoPath)                   // 从 PhotoData 提取照片路径
            put(COLUMN_COORDINATES, photoData.coordinates)                // 从 PhotoData 提取经纬度
            put(COLUMN_PITCH, photoData.pitch)                            // 从 PhotoData 提取俯仰角
            put(COLUMN_AZIMUTH, photoData.azimuth)                       // 从 PhotoData 提取方位角
            put(COLUMN_BUILDING_NAME, photoData.buildingName)            // 从 PhotoData 提取建筑物名称
            put(COLUMN_BUILDING_COORDINATES, photoData.buildingCoordinates) // 从 PhotoData 提取建筑物坐标
        }

        return try {
            db.insert(TABLE_NAME, null, contentValues) // 返回插入行的 ID
        } catch (e: Exception) {
            Log.e("DatabaseError", "Insert failed: ${e.message}")
            -1 // 返回 -1 表示插入失败
        } finally {
            db.close() // 确保数据库连接被关闭
        }
    }

    fun getAllData(): List<PhotoData> {
        val dataList = mutableListOf<PhotoData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val photoPathIndex = cursor.getColumnIndex(COLUMN_PHOTO_PATH)
                val coordinatesIndex = cursor.getColumnIndex(COLUMN_COORDINATES)
                val pitchIndex = cursor.getColumnIndex(COLUMN_PITCH)
                val azimuthIndex = cursor.getColumnIndex(COLUMN_AZIMUTH)
                val buildingNameIndex = cursor.getColumnIndex(COLUMN_BUILDING_NAME)
                val buildingCoordinatesIndex = cursor.getColumnIndex(COLUMN_BUILDING_COORDINATES)

                // 检查索引是否有效
                if (idIndex >= 0 && photoPathIndex >= 0 && coordinatesIndex >= 0 &&
                    pitchIndex >= 0 && azimuthIndex >= 0 &&
                    buildingNameIndex >= 0 && buildingCoordinatesIndex >= 0) {

                    val id = cursor.getLong(idIndex)
                    val photoPath = cursor.getString(photoPathIndex)
                    val coordinates = cursor.getString(coordinatesIndex)
                    val pitch = cursor.getString(pitchIndex)
                    val azimuth = cursor.getString(azimuthIndex)
                    val buildingName = cursor.getString(buildingNameIndex)
                    val buildingCoordinates = cursor.getString(buildingCoordinatesIndex)

                    val photoData = PhotoData(id, photoPath, coordinates, pitch, azimuth, buildingName, buildingCoordinates)
                    dataList.add(photoData)
                } else {
                    // 处理列未找到的情况
                    // 可以选择记录日志或抛出异常
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return dataList
    }
}