package com.example.camera

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhotoDataAdapter(
    private val photoDataList: MutableList<PhotoData>, // 使用 MutableList 以便修改列表
    private val onItemClick: (PhotoData) -> Unit,
    private val onDeleteClick: (PhotoData) -> Unit // 新增删除操作回调
) : RecyclerView.Adapter<PhotoDataAdapter.PhotoDataViewHolder>() {

    inner class PhotoDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val buildingTextView: TextView = itemView.findViewById(R.id.building_name_text_view)
        private val coordinatesTextView: TextView = itemView.findViewById(R.id.coordinates_text_view)
        private val pitchTextView: TextView = itemView.findViewById(R.id.pitch_text_view)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(photoData: PhotoData) {
            buildingTextView.text = photoData.buildingName
            coordinatesTextView.text = photoData.coordinates
            pitchTextView.text = "俯仰角度：${photoData.pitch}"

            // 设置收藏按钮颜色
            if (photoData.isFavorite) {
                favoriteButton.setColorFilter(Color.YELLOW) // 收藏时变为黄色
            } else {
                favoriteButton.setColorFilter(Color.GRAY) // 默认颜色
            }

            // 收藏按钮点击事件
            favoriteButton.setOnClickListener {
                photoData.isFavorite = !photoData.isFavorite
                if (photoData.isFavorite) {
                    favoriteButton.setColorFilter(Color.YELLOW)
                } else {
                    favoriteButton.setColorFilter(Color.GRAY)
                }

                // 如果是收藏，将条目移动到顶部
                if (photoData.isFavorite) {
                    photoDataList.removeAt(adapterPosition)
                    photoDataList.add(0, photoData)
                }

                // 更新 UI
                notifyItemMoved(adapterPosition, 0)
            }

            // 删除按钮点击事件
            deleteButton.setOnClickListener {
                onDeleteClick(photoData) // 调用删除回调
                notifyItemRemoved(adapterPosition) // 通知删除
            }

            itemView.setOnClickListener { onItemClick(photoData) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoDataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_data, parent, false)
        return PhotoDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoDataViewHolder, position: Int) {
        holder.bind(photoDataList[position])
    }

    override fun getItemCount(): Int = photoDataList.size
}