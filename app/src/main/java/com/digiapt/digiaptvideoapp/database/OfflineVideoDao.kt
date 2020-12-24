package com.digiapt.digiaptvideoapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digiapt.digiaptvideoapp.database.entities.OfflineVideo

@Dao
interface OfflineVideoDao {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun saveOfflineVideos(offlineVideos : List<OfflineVideo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOfflineVideo(offlineVideo : OfflineVideo)

//    @Query("SELECT * FROM OfflineVideo")
//    fun getOfflineVideos() : LiveData<List<OfflineVideo>>

    @Query("SELECT * FROM OfflineVideo WHERE mediaId LIKE :media_id ")
    fun getOfflineVideo(media_id : String) : OfflineVideo
}