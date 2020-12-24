package com.digiapt.digiaptvideoapp.listeners

import android.view.View
import com.digiapt.digiaptvideoapp.models.Section

interface SectionClickListener {
    fun onSectionItemClick(view: View, section: Section)
}