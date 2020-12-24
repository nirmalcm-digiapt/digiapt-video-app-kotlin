package com.digiapt.digiaptvideoapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.models.Section
import com.digiapt.digiaptvideoapp.databinding.AdapterSectionBinding
import com.digiapt.digiaptvideoapp.fragments.HomeFragment
import com.digiapt.digiaptvideoapp.listeners.SectionClickListener
import com.digiapt.digiaptvideoapp.listeners.VideoClickListener

class SectionAdapter (
    private val fragment: HomeFragment,
    private val viewLifecycleOwner: LifecycleOwner,
    private val sections: List<Section>,
    private val clickListener: SectionClickListener,
    private val videoClickListener: VideoClickListener
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>(){

    override fun getItemCount() = sections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SectionViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.adapter_section,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.recyclerviewBinding.section = sections[position]
        holder.recyclerviewBinding.root.setOnClickListener {
            clickListener.onSectionItemClick(holder.recyclerviewBinding.root, sections[position])
        }

        for (section in sections){
            if (section == sections[position]){
                holder.recyclerviewBinding.recyclerViewMovies.also {
                    it.layoutManager = LinearLayoutManager(fragment.context,RecyclerView.HORIZONTAL,false)
                    it.setHasFixedSize(true)
                    it.adapter =
                        VideoAdapter(
                            fragment,
                            section.section_videos,
                            videoClickListener
                        )
                }
            }
        }
    }

    inner class SectionViewHolder(
        val recyclerviewBinding: AdapterSectionBinding
    ) : RecyclerView.ViewHolder(recyclerviewBinding.root)

}