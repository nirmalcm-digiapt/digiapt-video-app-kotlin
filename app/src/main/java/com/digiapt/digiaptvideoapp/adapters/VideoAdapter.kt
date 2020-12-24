package com.digiapt.digiaptvideoapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.models.Video
import com.digiapt.digiaptvideoapp.databinding.AdapterVideoBinding
import com.digiapt.digiaptvideoapp.fragments.HomeFragment
import com.digiapt.digiaptvideoapp.listeners.VideoClickListener

class VideoAdapter (
    private val fragment: HomeFragment,
    private val videos: List<Video>,
    private val listener: VideoClickListener
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>(){

    override fun getItemCount() = videos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VideoViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.adapter_video,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.recyclerviewBinding.video = videos[position]
        holder.recyclerviewBinding.root.setOnClickListener {
            listener.onVideoItemClick(holder.recyclerviewBinding.root, videos[position])
        }

        var imageViewUrl = holder.recyclerviewBinding.video?.poster
        Log.d("imageViewurl","$imageViewUrl")
        Glide.with(fragment) //1
            .load(imageViewUrl)
            //.skipMemoryCache(true) //2
            .diskCacheStrategy(DiskCacheStrategy.ALL) //3
            //.transform(CircleCrop()) //4
            .into(holder.recyclerviewBinding.movieImage)
    }


    inner class VideoViewHolder(
        val recyclerviewBinding: AdapterVideoBinding
    ) : RecyclerView.ViewHolder(recyclerviewBinding.root)

}