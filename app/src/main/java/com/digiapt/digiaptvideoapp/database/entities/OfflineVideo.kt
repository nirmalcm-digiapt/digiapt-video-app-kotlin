package com.digiapt.digiaptvideoapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OfflineVideo(
    @PrimaryKey(autoGenerate = false)
    var mediaId: String
)