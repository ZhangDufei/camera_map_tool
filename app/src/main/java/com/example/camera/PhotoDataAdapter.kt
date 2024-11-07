package com.example.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhotoDataAdapter(
    private val photoDataList: List<PhotoData>,
    private val onItemClick: (PhotoData) -> Unit
) : RecyclerView.Adapter<PhotoDataAdapter.PhotoDataViewHolder>() {

    inner class PhotoDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coordinatesTextView: TextView = itemView.findViewById(R.id.coordinates_text_view)
        private val pitchTextView: TextView = itemView.findViewById(R.id.pitch_text_view)

        fun bind(photoData: PhotoData) {
            coordinatesTextView.text = photoData.coordinates
            pitchTextView.text = "俯仰角度：${photoData.pitch}"
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