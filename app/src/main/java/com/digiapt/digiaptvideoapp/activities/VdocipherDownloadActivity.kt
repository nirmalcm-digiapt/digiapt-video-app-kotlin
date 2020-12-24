package com.digiapt.digiaptvideoapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.modules.vdocipher.OptionSelector
import com.digiapt.digiaptvideoapp.modules.vdocipher.Utils.getSizeString
import com.digiapt.digiaptvideoapp.modules.vdocipher.VdocipherDownloadNotificationService

import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.offline.DownloadOptions
import com.vdocipher.aegis.offline.DownloadRequest
import com.vdocipher.aegis.offline.DownloadSelections
import com.vdocipher.aegis.offline.DownloadStatus
import com.vdocipher.aegis.offline.OptionsDownloader
import com.vdocipher.aegis.offline.VdoDownloadManager
import com.vdocipher.aegis.player.VdoPlayer

import java.io.File
import java.util.ArrayList
import java.util.Arrays

class VdocipherDownloadActivity : Activity(), VdoDownloadManager.EventListener {

    private var download1: Button? = null
    private var download2: Button? = null
    private var download3: Button? = null
    private var deleteAll: Button? = null
    private var refreshList: Button? = null
    private var downloadsListView: RecyclerView? = null

    // dataset which backs the adapter for downloads recyclerview
    private var downloadStatusList: ArrayList<DownloadStatus>? = null
    private var downloadStatusList_ : List<DownloadStatus>? = null

    private var downloadsAdapter: DownloadsAdapter? = null

    @Volatile
    private var vdoDownloadManager: VdoDownloadManager? = null

    private val optionsSelectedCallback = object :
        OptionSelector.OptionsSelectedCallback {
        override fun onTracksSelected(downloadOptions: DownloadOptions, selectedTracks: IntArray) {
            Log.i(
                TAG,
                selectedTracks.size.toString() + " options selected: " + Arrays.toString(
                    selectedTracks
                )
            )
            val durationMs = downloadOptions.mediaInfo.duration
            Log.i(TAG, "---- selected tracks ----")
            for (trackIndex in selectedTracks) {
                Log.i(
                    TAG,
                    getDownloadItemName(
                        downloadOptions.availableTracks[trackIndex],
                        durationMs
                    )
                )
            }
            Log.i(TAG, "---- selected tracks ----")

            // currently only (1 video + 1 audio) track supported
            if (selectedTracks.size != 2) {
                showToastAndLog("Invalid selection", Toast.LENGTH_LONG)
                return
            }

            downloadSelectedOptions(downloadOptions, selectedTracks)

            // disable the corresponding download button
            if (downloadOptions.mediaId == MEDIA_ID_1) download1!!.isEnabled = false
            if (downloadOptions.mediaId == MEDIA_ID_2) download2!!.isEnabled = false
            if (downloadOptions.mediaId == MEDIA_ID_3) download3!!.isEnabled = false
        }
    }

    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        download1 = findViewById(R.id.download_btn_1)
        download2 = findViewById(R.id.download_btn_2)
        download3 = findViewById(R.id.download_btn_3)
        download1!!.isEnabled = false
        download2!!.isEnabled = false
        download3!!.isEnabled = false
        downloadsListView = findViewById(R.id.downloads_list)

        deleteAll = findViewById(R.id.delete_all)
        deleteAll!!.isEnabled = false
        refreshList = findViewById(R.id.refresh_list)

        downloadStatusList = ArrayList()
        downloadsAdapter = DownloadsAdapter(downloadStatusList!!)
        downloadsListView!!.adapter = downloadsAdapter
        downloadsListView!!.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

//        downloads_list.also {
//            it.layoutManager = LinearLayoutManager(this)
//            it.setHasFixedSize(true)
//            it.adapter = downloadsAdapter
//        }

        refreshList!!.setOnClickListener { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                refreshDownloadsList()
            } else {
                showToastAndLog("Minimum api level required is 21", Toast.LENGTH_LONG)
            }
        }

        deleteAll!!.setOnClickListener { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                deleteAllDownloads()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            refreshDownloadsList()
        } else {
            showToastAndLog("Minimum api level required is 21", Toast.LENGTH_LONG)
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            maybeCreateManager()
            vdoDownloadManager!!.addEventListener(this)
        }
    }

    override fun onStop() {
        if (vdoDownloadManager != null) {
            vdoDownloadManager!!.removeEventListener(this)
        }
        startNotificationService()
        super.onStop()
    }

    // VdoDownloadManager.EventListener implementation

    override fun onQueued(mediaId: String, downloadStatus: DownloadStatus) {
        showToastAndLog("Download queued : $mediaId", Toast.LENGTH_SHORT)
        addListItem(downloadStatus)
    }

    override fun onChanged(mediaId: String, downloadStatus: DownloadStatus) {
        Log.d(
            TAG,
            "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%"
        )
        updateListItem(downloadStatus)
    }

    override fun onCompleted(mediaId: String, downloadStatus: DownloadStatus) {
        showToastAndLog("Download complete: $mediaId", Toast.LENGTH_SHORT)
        updateListItem(downloadStatus)
    }

    override fun onFailed(mediaId: String, downloadStatus: DownloadStatus) {
        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason)
        Toast.makeText(
            this, " download error: " + downloadStatus.reason,
            Toast.LENGTH_LONG
        ).show()
        updateListItem(downloadStatus)
    }

    override fun onDeleted(mediaId: String) {
        showToastAndLog("Deleted $mediaId", Toast.LENGTH_SHORT)
        removeListItem(mediaId)
    }

    // Private

    private fun startNotificationService() {
        startService(Intent(this, VdocipherDownloadNotificationService::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun maybeCreateManager() {
        if (vdoDownloadManager == null) {
            vdoDownloadManager = VdoDownloadManager.getInstance(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun refreshDownloadsList() {
        maybeCreateManager()
        vdoDownloadManager!!.query(VdoDownloadManager.Query(),
            VdoDownloadManager.QueryResultListener { statusList ->
                // enable sample download buttons for media not downloaded or queued
                if (!containsMediaId(statusList,
                        MEDIA_ID_1
                    ))
                    setDownloadListeners(download1, "sample 1",
                        OTP_1,
                        PLAYBACK_INFO_1
                    )
                if (!containsMediaId(statusList,
                        MEDIA_ID_2
                    ))
                    setDownloadListeners(download2, "sample 2",
                        OTP_2,
                        PLAYBACK_INFO_2
                    )
                if (!containsMediaId(statusList,
                        MEDIA_ID_3
                    ))
                    setDownloadListeners(download3, "sample 3",
                        OTP_3,
                        PLAYBACK_INFO_3
                    )

                // notify recyclerview
                downloadStatusList!!.clear()
                downloadStatusList!!.addAll(statusList)

                updateDeleteAllButton()
                downloadsAdapter!!.notifyDataSetChanged()

                if (statusList.isEmpty()) {
                    Log.w(TAG, "No query results found")
                    Toast.makeText(
                        this@VdocipherDownloadActivity,
                        "No query results found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@QueryResultListener
                }
                Log.i(TAG, statusList.size.toString() + " results found")

                val builder = StringBuilder()
                builder.append("query results:").append("\n")
                for (status in statusList) {
                    builder.append(
                        statusString(
                            status
                        )
                    ).append(" : ")
                        .append(status.mediaInfo.mediaId).append(", ")
                        .append(status.mediaInfo.title).append("\n")
                }
                Log.i(TAG, builder.toString())
            })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun deleteAllDownloads() {
        if (!downloadStatusList!!.isEmpty()) {
            maybeCreateManager()
            val mediaIdList = ArrayList<String>()
            for (status in downloadStatusList!!) {
                mediaIdList.add(status.mediaInfo.mediaId)
            }
            val mediaIds = mediaIdList.toTypedArray()
            vdoDownloadManager!!.remove(*mediaIds)
        }
    }

    private fun containsMediaId(statusList: List<DownloadStatus>, mediaId: String): Boolean {
        for (status in statusList) {
            if (status.mediaInfo.mediaId == mediaId) return true
        }
        return false
    }

    private fun setDownloadListeners(
        downloadButton: Button?, mediaName: String,
        otp: String, playbackInfo: String
    ) {
        runOnUiThread {
            downloadButton!!.isEnabled = true
            downloadButton.text = "Download $mediaName"
            downloadButton.setOnClickListener { view -> getOptions(otp, playbackInfo) }
        }
    }

    private fun getOptions(otp: String, playbackInfo: String) {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        Handler(handlerThread.looper).post {
            OptionsDownloader().downloadOptionsWithOtp(
                otp, playbackInfo, object : OptionsDownloader.Callback {
                    override fun onOptionsReceived(options: DownloadOptions) {
                        Log.i(TAG, "onOptionsReceived")
                        showSelectionDialog(options, options.mediaInfo.duration)
                    }

                    override fun onOptionsNotReceived(errDesc: ErrorDescription) {
                        val errMsg = "onOptionsNotReceived : $errDesc"
                        Log.e(TAG, errMsg)
                        Toast.makeText(this@VdocipherDownloadActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    fun showSelectionDialog(downloadOptions: DownloadOptions, durationMs: Long) {
        OptionSelector(
            downloadOptions,
            durationMs,
            optionsSelectedCallback,
            OptionSelector.OptionStyle.SHOW_HIGHEST_AND_LOWEST_QUALITY
        )
            .showSelectionDialog(this, "Download options")
    }

    private fun downloadSelectedOptions(
        downloadOptions: DownloadOptions,
        selectionIndices: IntArray
    ) {
        val selections = DownloadSelections(downloadOptions, selectionIndices)

        // ensure external storage is in read-write mode
        if (!isExternalStorageWritable) {
            showToastAndLog("External storage is not available", Toast.LENGTH_LONG)
            return
        }

        val downloadLocation: String
        try {
            downloadLocation = getExternalFilesDir(null)!!.path + File.separator + "offlineVdos"
        } catch (npe: NullPointerException) {
            Log.e(TAG, "external storage not available: " + Log.getStackTraceString(npe))
            Toast.makeText(this, "external storage not available", Toast.LENGTH_LONG).show()
            return
        }

        // ensure download directory is created
        val dlLocation = File(downloadLocation)
        if (!(dlLocation.exists() && dlLocation.isDirectory)) {
            // directory not created yet; let's create it
            if (!dlLocation.mkdir()) {
                Log.e(TAG, "failed to create storage directory")
                Toast.makeText(this, "failed to create storage directory", Toast.LENGTH_LONG).show()
                return
            }
        }

        Log.i(TAG, "will save media to $downloadLocation")

        // build a DownloadRequest
        val request = DownloadRequest.Builder(selections, downloadLocation).build()

        // enqueue request to VdoDownloadManager for download
        try {
            vdoDownloadManager!!.enqueue(request)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "error enqueuing download request")
            Toast.makeText(this, "error enqueuing download request", Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "error enqueuing download request")
            Toast.makeText(this, "error enqueuing download request", Toast.LENGTH_LONG).show()
        }

    }

    private fun showItemSelectedDialog(downloadStatus: DownloadStatus) {
        val builder = AlertDialog.Builder(this@VdocipherDownloadActivity)
        builder.setTitle(downloadStatus.mediaInfo.title)
            .setMessage("Status: " + statusString(
                downloadStatus
            ).toUpperCase())

        if (downloadStatus.status == VdoDownloadManager.STATUS_COMPLETED) {
            builder.setPositiveButton("PLAY") { dialog, which ->
                startPlayback(downloadStatus)
                dialog.dismiss()
            }
        } else {
            builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }
        }
        builder.setNegativeButton("DELETE") { dialog, which ->
            vdoDownloadManager!!.remove(downloadStatus.mediaInfo.mediaId)
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun updateListItem(status: DownloadStatus) {
        // if media already in downloadStatusList, update it
        val mediaId = status.mediaInfo.mediaId
        var position = -1
        for (i in downloadStatusList!!.indices) {
            if (downloadStatusList!![i].mediaInfo.mediaId == mediaId) {
                position = i
                break
            }
        }
        if (position >= 0) {
            downloadStatusList!![position] = status
            downloadsAdapter!!.notifyItemChanged(position)
        } else {
            Log.e(TAG, "item not found in adapter")
        }
        updateDeleteAllButton()
    }

    private fun addListItem(downloadStatus: DownloadStatus) {
        downloadStatusList!!.add(0, downloadStatus)
        updateDeleteAllButton()
        downloadsAdapter!!.notifyItemInserted(0)
    }

    private fun removeListItem(status: DownloadStatus) {
        // remove by comparing mediaId; status may change
        val mediaId = status.mediaInfo.mediaId
        removeListItem(mediaId)
    }

    private fun removeListItem(mediaId: String) {
        var position = -1
        for (i in downloadStatusList!!.indices) {
            if (downloadStatusList!![i].mediaInfo.mediaId == mediaId) {
                position = i
                break
            }
        }
        if (position >= 0) {
            downloadStatusList!!.removeAt(position)
            downloadsAdapter!!.notifyItemRemoved(position)
        }
        updateDeleteAllButton()
    }

    private fun updateDeleteAllButton() {
        deleteAll!!.isEnabled = !downloadStatusList!!.isEmpty()
    }

    private fun startPlayback(downloadStatus: DownloadStatus) {
        if (downloadStatus.status != VdoDownloadManager.STATUS_COMPLETED) {
            showToastAndLog("Download not complete", Toast.LENGTH_SHORT)
            return
        }
        val intent = Intent(this, VdocipherPlayerActivity::class.java)
        val vdoParams =
            VdoPlayer.VdoInitParams.createParamsForOffline(downloadStatus.mediaInfo.mediaId)
        intent.putExtra(VdocipherPlayerActivity.EXTRA_VDO_PARAMS, vdoParams)
        startActivity(intent)
    }

    private fun showToastAndLog(message: String, toastLength: Int) {
        runOnUiThread { Toast.makeText(applicationContext, message, toastLength).show() }
        Log.i(TAG, message)
    }

    private inner class DownloadsAdapter(private val statusList: List<DownloadStatus>) :
        RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var title: TextView
            var status: TextView

            init {
                title = itemView.findViewById(R.id.vdo_title)
                status = itemView.findViewById(R.id.download_status)
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val status = statusList[position]
                    showItemSelectedDialog(status)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.sample_list_item, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val status = statusList[position]
            holder.title.text = status.mediaInfo.title
            holder.status.text = statusString(
                status
            ).toUpperCase()
        }

        override fun getItemCount(): Int {
            return statusList.size
        }
    }

//    private inner class VdoDownloadsAdapter (
//        private var statusList: List<DownloadStatus>
//    ) : RecyclerView.Adapter<VdoDownloadsAdapter.VideoViewHolder>(){
//
//        override fun getItemCount() = statusList.size
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
//            VideoViewHolder(
//                DataBindingUtil.inflate(
//                    LayoutInflater.from(parent.context),
//                    R.layout.sample_list_item,
//                    parent,
//                    false
//                )
//            )
//
//        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
//            holder.recyclerviewBinding.root.setOnClickListener {
//                if (position != RecyclerView.NO_POSITION) {
//                    val status = statusList[position]
//                    showItemSelectedDialog(status)
//                }
//            }
//
//            holder.recyclerviewBinding.vdoTitle.text = statusList[position].mediaInfo.title
//            holder.recyclerviewBinding.downloadStatus.text = VdocipherDownloadActivity.statusString(statusList[position]).toUpperCase()
//        }
//
//
//        inner class VideoViewHolder(
//            val recyclerviewBinding: SampleListItemBinding
//        ) : RecyclerView.ViewHolder(recyclerviewBinding.root){
//        }
//
//    }

    companion object {
        private val TAG = "DownloadsActivity"

        // some samples for download demo
        private val SAMPLE_NAME_1 = "VdoCipher product demo"
        private val MEDIA_ID_1 = "661f6861d521a24288d608923d2c73f9"
        private val PLAYBACK_INFO_1 =
            "eyJ2aWRlb0lkIjoiNjYxZjY4NjFkNTIxYTI0Mjg4ZDYwODkyM2QyYzczZjkifQ=="
        private val OTP_1 = "20160313versASE313WAGCdGbRSkojp0pMJpESFT9RVVrbGSnzwVOr2ANUxMrfZ5"

        val SAMPLE_NAME_2 = "Home page video"
        val MEDIA_ID_2 = "3f29b5434a5c615cda18b16a6232fd75"
        val PLAYBACK_INFO_2 = "eyJ2aWRlb0lkIjoiM2YyOWI1NDM0YTVjNjE1Y2RhMThiMTZhNjIzMmZkNzUifQ=="
        val OTP_2 = "20160313versASE313BlEe9YKEaDuju5J0XcX2Z03Hrvm5rzKScvuyojMSBZBxfZ"

        private val SAMPLE_NAME_3 = "Tears of steel"
        private val MEDIA_ID_3 = "5392515b761ef71e8c00a2301e1cece3"
        private val PLAYBACK_INFO_3 =
            "eyJ2aWRlb0lkIjoiNTM5MjUxNWI3NjFlZjcxZThjMDBhMjMwMWUxY2VjZTMifQ=="
        private val OTP_3 = "20160313versASE313TKtOGPa5FImICHI4Q62Gkmj41zyTBlAOV8V2uLVgMUPYgT"

        private fun getDownloadItemName(track: Track, durationMs: Long): String {
            val type =
                if (track.type == Track.TYPE_VIDEO) "V" else if (track.type == Track.TYPE_AUDIO) "A" else "?"
            return type + " " + track.bitrate / 1024 + " kbps, " +
                    getSizeString(track.bitrate, durationMs)
        }

        private fun statusString(status: DownloadStatus): String {
            when (status.status) {
                VdoDownloadManager.STATUS_COMPLETED -> return "Completed"
                VdoDownloadManager.STATUS_FAILED -> return "Error " + status.reason
                VdoDownloadManager.STATUS_PENDING -> return "Queued"
                VdoDownloadManager.STATUS_PAUSED -> return "Paused " + status.downloadPercent + "%"
                VdoDownloadManager.STATUS_DOWNLOADING -> return "Downloading " + status.downloadPercent + "%"
                else -> return "Not found"
            }
        }
    }
}
