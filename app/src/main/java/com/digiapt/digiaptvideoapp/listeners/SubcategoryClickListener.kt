package com.digiapt.digiaptvideoapp.listeners

import android.view.View
import com.digiapt.digiaptvideoapp.models.Subcategory

interface SubcategoryClickListener {
    fun onSubcategoryItemClick(view: View, subcategory: Subcategory)
}