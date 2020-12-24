package com.digiapt.digiaptvideoapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.models.Subcategory
import com.digiapt.digiaptvideoapp.databinding.AdapterSubcategoryBinding
import com.digiapt.digiaptvideoapp.fragments.HomeFragment
import com.digiapt.digiaptvideoapp.listeners.SubcategoryClickListener

class SubcategoryAdapter (
    private val fragment: HomeFragment,
    private val viewLifecycleOwner: LifecycleOwner,
    private val subcategories: List<Subcategory>,
    private val clickListener: SubcategoryClickListener
) : RecyclerView.Adapter<SubcategoryAdapter.SectionViewHolder>(){

    override fun getItemCount() = subcategories.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SectionViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.adapter_subcategory,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.recyclerviewBinding.subcategory = subcategories[position]
        holder.recyclerviewBinding.root.setOnClickListener {
            clickListener.onSubcategoryItemClick(holder.recyclerviewBinding.root, subcategories[position])
        }

    }

    inner class SectionViewHolder(
        val recyclerviewBinding: AdapterSubcategoryBinding
    ) : RecyclerView.ViewHolder(recyclerviewBinding.root)

}