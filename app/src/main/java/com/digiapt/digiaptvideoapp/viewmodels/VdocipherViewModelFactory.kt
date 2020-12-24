package com.digiapt.digiaptvideoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.digiapt.digiaptvideoapp.repositories.VdocipherRepository

@Suppress("UNCHECKED_CAST")
class VdocipherViewModelFactory(
    private val repository: VdocipherRepository
) : ViewModelProvider.NewInstanceFactory(){

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return VdocipherViewModel(repository) as T
    }

}