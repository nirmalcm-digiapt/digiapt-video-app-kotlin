package com.digiapt.digiaptvideoapp.activities

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.digiapt.digiaptvideoapp.R
import com.digiapt.digiaptvideoapp.modules.vdocipher.Utils
import com.digiapt.digiaptvideoapp.modules.vdocipher.VdoPlayerControlView
import com.digiapt.digiaptvideoapp.network.VdocipherApi
import com.digiapt.digiaptvideoapp.repositories.VdocipherRepository
import com.digiapt.digiaptvideoapp.viewmodels.VdocipherViewModel
import com.digiapt.digiaptvideoapp.viewmodels.VdocipherViewModelFactory

import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoPlayer
import com.vdocipher.aegis.player.VdoPlayer.VdoInitParams
import com.vdocipher.aegis.player.VdoPlayerSupportFragment
import kotlinx.android.synthetic.main.activity_player_vdocipher.*

class VdocipherPlayerActivity : AppCompatActivity(), VdoPlayer.InitializationListener {

    lateinit private var videoId : String

    private var playerFragment: VdoPlayerSupportFragment? = null
    private var playerControlView: VdoPlayerControlView? = null
    private var eventLog: TextView? = null

    private lateinit var factory: VdocipherViewModelFactory
    private lateinit var viewModel: VdocipherViewModel

    private var eventLogString = ""
    private var currentOrientation: Int = 0
    private var vdoParams: VdoInitParams? = null

    private val playbackListener = object : VdoPlayer.PlaybackEventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            log(
                Utils.playbackStateString(
                    playWhenReady,
                    playbackState
                )
            )
        }

        override fun onTracksChanged(tracks: Array<Track>, tracks1: Array<Track>) {
            Log.i(TAG, "onTracksChanged")
            log("onTracksChanged")
        }

        override fun onBufferUpdate(bufferTime: Long) {}

        override fun onSeekTo(millis: Long) {
            Log.i(TAG, "onSeekTo: $millis")
        }

        override fun onProgress(millis: Long) {}

        override fun onPlaybackSpeedChanged(speed: Float) {
            Log.i(TAG, "onPlaybackSpeedChanged $speed")
            log("onPlaybackSpeedChanged $speed")
        }

        override fun onLoading(vdoInitParams: VdoPlayer.VdoInitParams) {
            Log.i(TAG, "onLoading")
            log("onLoading")
        }

        override fun onLoadError(
            vdoInitParams: VdoPlayer.VdoInitParams,
            errorDescription: ErrorDescription
        ) {
            val err = "onLoadError code: " + errorDescription.errorCode
            Log.e(TAG, err)
            log(err)
        }

        override fun onLoaded(vdoInitParams: VdoPlayer.VdoInitParams) {
            Log.i(TAG, "onLoaded")
            log("onLoaded")
        }

        override fun onError(
            vdoParams: VdoPlayer.VdoInitParams,
            errorDescription: ErrorDescription
        ) {
            val err =
                "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg
            Log.e(TAG, err)
            log(err)
        }

        override fun onMediaEnded(vdoInitParams: VdoPlayer.VdoInitParams) {
            Log.i(TAG, "onMediaEnded")
            log("onMediaEnded")
        }
    }

    private val fullscreenToggleListener = object :
        VdoPlayerControlView.FullscreenActionListener {
        override fun onFullscreenAction(enterFullscreen: Boolean): Boolean{
            showFullScreen(enterFullscreen)
            return true
        }
    }

    private val visibilityListener = object :
        VdoPlayerControlView.ControllerVisibilityListener {
        override fun onControllerVisibilityChange(visibility: Int) {
            Log.i(TAG, "controller visibility $visibility")
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (visibility != View.VISIBLE) {
                    showSystemUi(false)
                }
            }
        }
    }

    private val uiVisibilityListener = View.OnSystemUiVisibilityChangeListener { visibility ->
        Log.v(TAG, "onSystemUiVisibilityChange")
        // show player controls when system ui is showing
        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            Log.v(TAG, "system ui visible, making controls visible")
            showControls(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate called")
        setContentView(R.layout.activity_player_vdocipher)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.setOnSystemUiVisibilityChangeListener(uiVisibilityListener)

        val window = getWindow()
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.grey_900))

        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }

        if (savedInstanceState != null) {
            vdoParams = savedInstanceState.getParcelable("initParams")
        }

        if (vdoParams == null) {
            vdoParams = intent.getParcelableExtra(EXTRA_VDO_PARAMS)
        }

        playerFragment =
            supportFragmentManager.findFragmentById(R.id.vdo_player_fragment) as VdoPlayerSupportFragment?
        playerControlView = findViewById(R.id.player_control_view)
        eventLog = findViewById(R.id.event_log)
        eventLog!!.movementMethod = ScrollingMovementMethod.getInstance()
        showControls(false)

        currentOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        initializePlayer()
    }

    override fun onStart() {
        Log.v(TAG, "onStart called")
        super.onStart()
    }

    override fun onStop() {
        Log.v(TAG, "onStop called")
        disablePlayerUI()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG, "onSaveInstanceState called")
        super.onSaveInstanceState(outState)
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams)
        }
    }

    private fun initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            playerFragment!!.initialize(this@VdocipherPlayerActivity)
            log("initializing player fragment")
        } else {
            // lets get otp and playbackInfo before creating the player
            obtainOtpAndPlaybackInfo()
        }
    }

    /**
     * Fetch (otp + playbackInfo) and initialize VdoPlayer
     * here we're fetching a sample (otp + playbackInfo)
     * TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the
     * video you wish to play
     */
    private fun obtainOtpAndPlaybackInfo() {
        // todo use asynctask
        log("fetching params...")

        lateinit var factory: VdocipherViewModelFactory
        lateinit var viewModel: VdocipherViewModel

        val api = VdocipherApi()
        val repository = VdocipherRepository(api)

        factory =
            VdocipherViewModelFactory(
                repository
            )
        viewModel = ViewModelProviders.of(this, factory).get(VdocipherViewModel::class.java)

        videoId = intent.getStringExtra("data")

        viewModel.getVideoOTP(videoId)

        viewModel.videoInfo.observe(this, Observer { videoInfo ->
            Log.d("MyLog otp",videoInfo.otp)
            Log.d("MyLog playbackInfo",videoInfo.playbackInfo)

            vdoParams = VdoInitParams.Builder()
                .setOtp(videoInfo.otp)
                .setPlaybackInfo(videoInfo.playbackInfo)
                .setPreferredCaptionsLanguage("en")
                .build()
            Log.i(TAG, "obtained new otp and playbackInfo")
            runOnUiThread { this.initializePlayer() }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this@VdocipherPlayerActivity, message, Toast.LENGTH_SHORT).show() }
    }

    private fun log(msg: String) {
        eventLogString += msg + "\n"
        eventLog!!.text = eventLogString
    }

    private fun showControls(show: Boolean) {
        Log.v(TAG, (if (show) "show" else "hide") + " controls")
        if (show) {
            playerControlView!!.show()
        } else {
            playerControlView!!.hide()
        }
    }

    private fun disablePlayerUI() {
        //        showControls(false);
    }

    override fun onInitializationSuccess(
        playerHost: VdoPlayer.PlayerHost,
        player: VdoPlayer,
        wasRestored: Boolean
    ) {
        Log.i(TAG, "onInitializationSuccess")
        log("onInitializationSuccess")
        player.addPlaybackEventListener(playbackListener)
        playerControlView!!.setPlayer(player)
        showControls(true)

        playerControlView!!.setFullscreenActionListener(fullscreenToggleListener)
        playerControlView!!.setControllerVisibilityListener(visibilityListener)

        // load a media to the player
        player.load(vdoParams)
        log("loaded init params to player")
    }

    override fun onInitializationFailure(
        playerHost: VdoPlayer.PlayerHost,
        errorDescription: ErrorDescription
    ) {
        val msg =
            "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg
        log(msg)
        Log.e(TAG, msg)
        Toast.makeText(
            this@VdocipherPlayerActivity,
            "initialization failure: " + errorDescription.errorMsg,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val newOrientation = newConfig.orientation
        val oldOrientation = currentOrientation
        currentOrientation = newOrientation
        Log.i(
            TAG, "new orientation " + if (newOrientation == Configuration.ORIENTATION_PORTRAIT)
                "PORTRAIT"
            else if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) "LANDSCAPE" else "UNKNOWN"
        )
        super.onConfigurationChanged(newConfig)
        if (newOrientation == oldOrientation) {
            Log.i(TAG, "orientation unchanged")
        } else if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // hide other views
            title_text.setVisibility(View.GONE)
            log_container.setVisibility(View.GONE)
//            R.id.vdo_player_fragment.setLayoutParams(
//                RelativeLayout.LayoutParams(
//                    RelativeLayout.LayoutParams.MATCH_PARENT,
//                    RelativeLayout.LayoutParams.MATCH_PARENT
//                ).apply { R.id.vdo_player_fragment }
//            )
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply { R.id.vdo_player_fragment }

            playerControlView!!.fitsSystemWindows = true
            // hide system windows
            showSystemUi(false)
            showControls(false)
        } else {
            // show other views
            title_text.setVisibility(View.VISIBLE)
            log_container.setVisibility(View.VISIBLE)
//            vdo_player_fragment.setLayoutParams(
//                RelativeLayout.LayoutParams(
//                    RelativeLayout.LayoutParams.MATCH_PARENT,
//                    RelativeLayout.LayoutParams.WRAP_CONTENT
//                )
//            )
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply { R.id.vdo_player_fragment }
            playerControlView!!.fitsSystemWindows = false
            playerControlView!!.setPadding(0, 0, 0, 0)
            // show system windows
            showSystemUi(true)
        }
    }

    override fun onBackPressed() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            showFullScreen(false)
            playerControlView!!.setFullscreenState(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun showFullScreen(show: Boolean) {
        Log.v(TAG, (if (show) "enter" else "exit") + " fullscreen")
        if (show) {
            // go to landscape orientation for fullscreen mode
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            // go to portrait orientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun showSystemUi(show: Boolean) {
        Log.v(TAG, (if (show) "show" else "hide") + " system ui")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!show) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    companion object {

        private val TAG = "VdocipherPlayerActivity"
        val EXTRA_VDO_PARAMS = "vdo_params"
    }
}
