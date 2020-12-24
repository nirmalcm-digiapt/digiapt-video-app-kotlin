package com.digiapt.digiaptvideoapp.modules.vdocipher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.util.Log
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.activities.VdocipherDownloadActivity

import com.vdocipher.aegis.offline.DownloadStatus
import com.vdocipher.aegis.offline.VdoDownloadManager

class VdocipherDownloadNotificationService:Service(), VdoDownloadManager.EventListener {

    private var vdoDownloadManager:VdoDownloadManager? = null
    private var mainHandler:Handler = Handler()

    private val checkIfNeeded = object:Runnable {
        override fun run() {
         // check if there is at least one active download
            val query = VdoDownloadManager.Query().setFilterByStatus(
            VdoDownloadManager.STATUS_PENDING, VdoDownloadManager.STATUS_DOWNLOADING)
            vdoDownloadManager!!.query(query
            ) {
                    list ->
                if (list.isEmpty()) {
                    // it's time to close
                    closeService()
                } else {
                    // reschedule
                    mainHandler.postDelayed(this, 10000)
                }
            }
        }
    }

    init{
        mainHandler = Handler(Looper.getMainLooper())
    }

    override fun onBind(intent:Intent):IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        if (Build.VERSION.SDK_INT < 21) {
            stopSelf()
        }
        else {
            createNotificationChannel()
            vdoDownloadManager = VdoDownloadManager.getInstance(this)
            vdoDownloadManager!!.addEventListener(this)
            startForeground(DOWNLOAD_NOTIFICATION_ID, getDownloadNotification("Downloads", "Tap to see downloads"))
            mainHandler.postDelayed(checkIfNeeded, 10000)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (vdoDownloadManager != null) {
            vdoDownloadManager!!.removeEventListener(this)
        }
        super.onDestroy()
    }

    private fun closeService() {
        Log.d(TAG, "closeService")
        stopForeground(false)
        stopSelf()
    }

    private fun updateNotification(downloadStatus:DownloadStatus) {
        val notification = getDownloadNotification(makeTitle(downloadStatus), makeDescription(downloadStatus))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager!!.notify(DOWNLOAD_NOTIFICATION_ID, notification)
    }

    private fun getDownloadNotification(title:String, description:String):Notification {
        val notificationIntent = Intent(this, VdocipherDownloadActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
        .setOnlyAlertOnce(true)
        .setSmallIcon(R.drawable.ic_file_download_black_18dp)
        .setContentTitle(title)
        .setContentText(description)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    }

    private fun createNotificationChannel() {
     // Create the NotificationChannel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download notifications"
            val description = "Download notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
             // Register the channel with the system; you can't change the importance
                        // or other notification behaviors after this
                        val notificationManager = getSystemService(NotificationManager::class.java!!)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun createDownloadsIntent() {
     //
    }

    private fun makeTitle(downloadStatus:DownloadStatus):String {
    val title = downloadStatus.mediaInfo.title
        when (downloadStatus.status) {
            VdoDownloadManager.STATUS_DOWNLOADING -> return "Downloading $title"
            VdoDownloadManager.STATUS_COMPLETED -> return "Downloaded $title"
            VdoDownloadManager.STATUS_FAILED -> return "Error downloading $title"
        else -> return title
        }
    }

    private fun makeDescription(downloadStatus:DownloadStatus):String {
        when (downloadStatus.status) {
            VdoDownloadManager.STATUS_DOWNLOADING -> return (downloadStatus.downloadPercent).toString() + "%"
            VdoDownloadManager.STATUS_COMPLETED -> return "100%"
            VdoDownloadManager.STATUS_FAILED -> return "Error code " + downloadStatus.reason
        else -> return ""
        }
    }

    // VdoDownloadManager.EventListener implementation

    override fun onQueued(mediaId:String, downloadStatus:DownloadStatus) {
        Log.d(TAG, "Download queued : $mediaId")
    }

    override fun onChanged(mediaId:String, downloadStatus:DownloadStatus) {
        Log.d(TAG, "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%")
        updateNotification(downloadStatus)
    }

    override fun onCompleted(mediaId:String, downloadStatus:DownloadStatus) {
        Log.d(TAG, "Download complete: $mediaId")
        updateNotification(downloadStatus)
    }

    override fun onFailed(mediaId:String, downloadStatus:DownloadStatus) {
        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason)
        updateNotification(downloadStatus)
    }

    override fun onDeleted(mediaId:String) {
        Log.d(TAG, "Deleted $mediaId")
    }

    companion object {
        private val TAG = "DownloadNotification"
        private val CHANNEL_ID = "downloads_notification_channel_name"
        private val DOWNLOAD_NOTIFICATION_ID = 100
    }
}
