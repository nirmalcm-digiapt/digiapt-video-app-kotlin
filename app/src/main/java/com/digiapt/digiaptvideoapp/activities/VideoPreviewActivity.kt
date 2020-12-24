package com.digiapt.digiaptvideoapp.activities

import android.content.Intent
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.database.entities.OfflineVideo
import com.digiapt.digiaptvideoapp.network.VdocipherApi
import com.digiapt.digiaptvideoapp.repositories.VdocipherRepository
import com.digiapt.digiaptvideoapp.databinding.ActivityVideoPreviewBinding
import com.digiapt.digiaptvideoapp.viewmodels.HomeViewModel
import com.digiapt.digiaptvideoapp.viewmodels.HomeViewModelFactory
import com.digiapt.digiaptvideoapp.modules.vdocipher.*
import com.digiapt.digiaptvideoapp.viewmodels.VdocipherViewModel
import com.digiapt.digiaptvideoapp.viewmodels.VdocipherViewModelFactory
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.offline.*
import com.vdocipher.aegis.player.VdoPlayer
import kotlinx.android.synthetic.main.activity_video_preview.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.File
import java.util.*


class VideoPreviewActivity : AppCompatActivity() , VdoDownloadManager.EventListener, KodeinAware{

    override val kodein by kodein()

    private lateinit var viewModel: HomeViewModel
    private val factory: HomeViewModelFactory by instance()

    lateinit var mMediaIdRecieved : String
    lateinit var mMediaNameRecieved : String
    lateinit var mMediaImagePathRecieved : String

    private var mIsDownloaded : Boolean = false
    var mOfflineVideo: OfflineVideo? = null

    var mDownloadStatus: DownloadStatus? = null

    lateinit var mOfflineVideoTemp: OfflineVideo

    @Volatile
    private var vdoDownloadManager: VdoDownloadManager? = null

    companion object {
        public val TAG = "DownloadsActivity"

        // some samples for download demo
        private val SAMPLE_NAME_1 = "VdoCipher product demo"
        private val MEDIA_ID_1 = "661f6861d521a24288d608923d2c73f9"
        private val PLAYBACK_INFO_1 =
            "eyJ2aWRlb0lkIjoiNjYxZjY4NjFkNTIxYTI0Mjg4ZDYwODkyM2QyYzczZjkifQ=="
        private val OTP_1 = "20160313versASE313WAGCdGbRSkojp0pMJpESFT9RVVrbGSnzwVOr2ANUxMrfZ5"

        private fun getDownloadItemName(track: Track, durationMs: Long): String {
            val type =
                if (track.type == Track.TYPE_VIDEO) "V" else if (track.type == Track.TYPE_AUDIO) "A" else "?"
            return type + " " + track.bitrate / 1024 + " kbps, " +
                    Utils.getSizeString(track.bitrate, durationMs)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        receiveIntent()

        initDataBinding()
        initViewModel()

        initStatusBar()
        initToolbar()
        initView()
    }

    private fun receiveIntent() {
        if(intent!=null){
            mMediaIdRecieved = intent.getStringExtra("video_id")
            mMediaImagePathRecieved = intent.getStringExtra("video_image_path")
            mMediaNameRecieved = intent.getStringExtra("video_name")
        }
    }

    private fun initDataBinding() {
        val binding:  ActivityVideoPreviewBinding = DataBindingUtil.setContentView(this, R.layout.activity_video_preview)
        binding.videoPreviewActivity = this
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, factory).get(HomeViewModel::class.java)
    }

    private fun initStatusBar() {
        val window = getWindow()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlack))
    }

    private fun initToolbar() {
        //setSupportActionBar(toolbar)
    }

    private fun initView() {
        Glide.with(this) //1
            .load(mMediaImagePathRecieved)
            //.skipMemoryCache(true) //2
            .diskCacheStrategy(DiskCacheStrategy.ALL) //3
            //.transform(CircleCrop()) //4
            .into(video_image)

        checkDownloadStatusAndUpdateUI()

        prepareMedia()
    }

    private fun checkDownloadStatusAndUpdateUI() {
//        offlineVideo = viewModel.getOfflineVideo(MEDIA_ID_1)
//        //offlineVideo = viewModel.offlineVideo
//
//        Log.d("ROOM DATABASE",offlineVideo.mediaId)
//        if(offlineVideo.mediaId == MEDIA_ID_1){
//            mIsDownloaded = true
//            updateDownloadUI(mIsDownloaded)
//        }

        mIsDownloaded = mDownloadStatus?.status == VdoDownloadManager.STATUS_COMPLETED

        updateDownloadUI()
    }

    private fun updateDownloadUI(){
        if(mIsDownloaded){
            id_download_text.text = "Downloaded"
            id_download_icon.setImageResource(R.drawable.ic_done)
        } else{
            id_download_text.text = "Download"
            id_download_icon.setImageResource(R.drawable.ic_download)
        }
    }

    private fun prepareMedia() {
        obtainOTPandPlaybackInfo()
    }

    private fun obtainOTPandPlaybackInfo() {
        lateinit var vdocipherFactory: VdocipherViewModelFactory
        lateinit var vdocipherViewModel: VdocipherViewModel

        val api = VdocipherApi()
        val repository = VdocipherRepository(api)

        vdocipherFactory =
            VdocipherViewModelFactory(
                repository
            )
        vdocipherViewModel = ViewModelProviders.of(this, vdocipherFactory).get(VdocipherViewModel::class.java)

        vdocipherViewModel.getVideoOTP(mMediaIdRecieved)

        vdocipherViewModel.videoInfo.observe(this, Observer { videoInfo ->
            Log.d("MyLog otp",videoInfo.otp)
            Log.d("MyLog playbackInfo",videoInfo.playbackInfo)

            id_download.setOnClickListener { view ->
                if(mIsDownloaded){
                    showItemSelectedDialog(mOfflineVideo!!)
                } else{
                    getOptions(
                        OTP_1,
                        PLAYBACK_INFO_1
                    )
                }
            }
        })
    }

    fun onClick(view: View){
        when(view.id){
            R.id.id_back -> {
                onBackPressed()
            }
            R.id.id_play ->{
                if(mIsDownloaded){
                    playOffline(mOfflineVideo!!.mediaId)
                } else {
                    playOnline(mMediaIdRecieved)
                }
            }
            else -> {
            }
        }
    }

    private fun playOffline(mediaId: String) {
        showToastAndLog("Playing offline video", Toast.LENGTH_LONG)
        Log.d(TAG,"playing offline video")
        val intent = Intent(this, VdocipherPlayerActivity::class.java)
        val vdoParams =
            VdoPlayer.VdoInitParams.createParamsForOffline(mediaId)
        intent.putExtra(VdocipherPlayerActivity.EXTRA_VDO_PARAMS, vdoParams)
        intent.putExtra("data","null")
        startActivity(intent)
    }

    private fun playOnline(mediaId: String) {
        showToastAndLog("Playing online video", Toast.LENGTH_LONG)
        Log.d(TAG,"playing online video")
        val intent = Intent(this, VdocipherPlayerActivity::class.java)
        intent.putExtra("data",mediaId)
        startActivity(intent)
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            maybeCreateManager()
            vdoDownloadManager!!.addEventListener(this)
        }
        checkDownloadStatusAndUpdateUI()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            maybeCreateManager()
            vdoDownloadManager!!.addEventListener(this)
        }
        checkDownloadStatusAndUpdateUI()
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
        showDownloadQueuedOrChangedUI(downloadStatus)

        showToastAndLog("Download queued : $mediaId", Toast.LENGTH_SHORT)
    }

    override fun onChanged(mediaId: String, downloadStatus: DownloadStatus) {
        showDownloadQueuedOrChangedUI(downloadStatus)

        Log.d(
            TAG,
            "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%"
        )
    }

    override fun onCompleted(mediaId: String, downloadStatus: DownloadStatus) {
        showDownloadCompletedorFailedorDeletedUI()

        showToastAndLog("Download complete: $mediaId", Toast.LENGTH_SHORT)

        val offlineVideo = OfflineVideo(mediaId)
        saveDownload(offlineVideo)

        showItemSelectedDialog(offlineVideo)

        /////////////////////////////////
        mDownloadStatus = downloadStatus
        mOfflineVideo = offlineVideo

        checkDownloadStatusAndUpdateUI()
    }

    override fun onFailed(mediaId: String, downloadStatus: DownloadStatus) {
        showDownloadCompletedorFailedorDeletedUI()

        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason)
        Toast.makeText(
            this, " download error: " + downloadStatus.reason,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDeleted(mediaId: String) {
        showDownloadCompletedorFailedorDeletedUI()

        showToastAndLog("Deleted $mediaId", Toast.LENGTH_SHORT)

        mDownloadStatus = null
        mOfflineVideo = null
        checkDownloadStatusAndUpdateUI()
    }

    private fun showDownloadQueuedOrChangedUI(downloadStatus: DownloadStatus) {
        id_download_notinprogress.visibility = View.INVISIBLE
        id_progress_bar.visibility = View.VISIBLE
        id_progress_bar.progress = downloadStatus.downloadPercent
        id_progress_bar_percentage.visibility = View.VISIBLE
        id_progress_bar_percentage.text = downloadStatus.downloadPercent.toString() + "%"
    }

    private fun showDownloadCompletedorFailedorDeletedUI(){
        id_download_notinprogress.visibility = View.VISIBLE
        id_progress_bar.visibility = View.INVISIBLE
        id_progress_bar_percentage.visibility = View.INVISIBLE
    }

    private fun saveDownload(offlineVideo: OfflineVideo) {
        viewModel.saveOfflineVideo(offlineVideo)
        checkDownloadStatusAndUpdateUI()
    }

    private fun deleteDownload(mediaId: String){
        checkDownloadStatusAndUpdateUI()
    }

    private val optionsSelectedCallback = object : OptionSelector.OptionsSelectedCallback {
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

        }
    }

    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
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
                        Toast.makeText(this@VideoPreviewActivity, errMsg, Toast.LENGTH_LONG).show()
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

//    private fun showItemSelectedDialog(downloadStatus: DownloadStatus) {
//        val builder = AlertDialog.Builder(this@VideoPreviewActivity)
//        builder.setTitle(downloadStatus.mediaInfo.title)
//            .setMessage("Status: " + statusString(downloadStatus).toUpperCase())
//
//        if (downloadStatus.status == VdoDownloadManager.STATUS_COMPLETED) {
//            builder.setPositiveButton("PLAY") { dialog, which ->
//                startPlayback(downloadStatus)
//                dialog.dismiss()
//            }
//        } else {
//            builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }
//        }
//        builder.setNegativeButton("DELETE") { dialog, which ->
//            vdoDownloadManager!!.remove(downloadStatus.mediaInfo.mediaId)
//            dialog.dismiss()
//        }
//
//        builder.create().show()
//    }

    private fun showItemSelectedDialog(offlineVideo: OfflineVideo) {
        val builder = AlertDialog.Builder(this@VideoPreviewActivity)
        builder.setTitle(offlineVideo.mediaId)
            .setMessage("Status: " + "Completed")

        builder.setPositiveButton("PLAY") { dialog, which ->
            playOffline(offlineVideo.mediaId)
            dialog.dismiss()
        }
        builder.setNegativeButton("DELETE") { dialog, which ->
            vdoDownloadManager!!.remove(offlineVideo.mediaId)
            dialog.dismiss()
        }

        builder.create().show()
    }

//    private fun startPlayback(downloadStatus: DownloadStatus) {
//        if (downloadStatus.status != VdoDownloadManager.STATUS_COMPLETED) {
//            showToastAndLog("Download not complete", Toast.LENGTH_SHORT)
//            return
//        }
//        val intent = Intent(this, VdocipherPlayerActivity::class.java)
//        val vdoParams =
//            VdoPlayer.VdoInitParams.createParamsForOffline(downloadStatus.mediaInfo.mediaId)
//        intent.putExtra(VdocipherPlayerActivity.EXTRA_VDO_PARAMS, vdoParams)
//        startActivity(intent)
//    }
//
//    private fun startPlayback(mediaId: String) {
//        val intent = Intent(this, VdocipherPlayerActivity::class.java)
//        val vdoParams =
//            VdoPlayer.VdoInitParams.createParamsForOffline(mediaId)
//        intent.putExtra(VdocipherPlayerActivity.EXTRA_VDO_PARAMS, vdoParams)
//        startActivity(intent)
//    }

    private fun showToastAndLog(message: String, toastLength: Int) {
        runOnUiThread { Toast.makeText(applicationContext, message, toastLength).show() }
        Log.i(TAG, message)
    }

}