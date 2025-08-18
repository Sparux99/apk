package com.amine.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // مكتبة لتحميل الصور بكفاءة

class VideoAdapter(
    private val videos: List<Video>,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video)
        holder.itemView.setOnClickListener { onVideoClick(video) }
    }

    override fun getItemCount() = videos.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var videos: List<Video>
        private val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val title: TextView = itemView.findViewById(R.id.video_title)
        private val dateAdded: TextView = itemView.findViewById(R.id.video_date_added)

        fun bind(video: Video) {
            title.text = video.title.substringBeforeLast('.')
            
            // استخدام Glide لتحميل الصورة المصغرة بكفاءة
            Glide.with(itemView.context)
                .load(video.contentUri)
                .centerCrop()
                .into(thumbnail)
        }

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        dateAdded.text = sdf.format(Date(video.dateAdded * 1000))

        Glide.with(itemView.context)
            .load(video.contentUri)
            .centerCrop()
            .into(thumbnail)
    }
}