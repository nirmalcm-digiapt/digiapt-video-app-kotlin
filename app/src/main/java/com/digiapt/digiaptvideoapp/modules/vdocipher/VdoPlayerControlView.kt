package com.digiapt.digiaptvideoapp.modules.vdocipher

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView

import com.digiapt.digiaptvideoapp.R
import com.vdocipher.aegis.media.ErrorDescription
import com.vdocipher.aegis.media.Track
import com.vdocipher.aegis.player.VdoPlayer

import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.ArrayList

/**
 * A view for controlling playback via a VdoPlayer.
 */
class VdoPlayerControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val playButton: View
    private val pauseButton: View
    private val fastForwardButton: View
    private val rewindButton: View
    private val durationView: TextView
    private val positionView: TextView
    private val seekBar: SeekBar
    private val speedControlButton: Button
    private val captionsButton: ImageButton
    private val qualityButton: ImageButton
    private val enterFullscreenButton: ImageButton
    private val exitFullscreenButton: ImageButton
    private val loaderView: ProgressBar
    private val errorView: ImageButton
    private val errorTextView: TextView
    private val controlPanel: View

    private val ffwdMs: Int
    private val rewindMs: Int
    private val showTimeoutMs: Int

    private var scrubbing: Boolean = false
    private var isAttachedToWindowOrNot: Boolean = false
    private var fullscreen: Boolean = false

    private var player: VdoPlayer? = null
    private val uiListener: UiListener
    private var lastErrorParams: VdoPlayer.VdoInitParams? =
        null // todo gather all relevant state and update UI using it
    private var fullscreenActionListener: FullscreenActionListener? = null
    private var visibilityListener: ControllerVisibilityListener? = null
    private var chosenSpeedIndex = 2

    private val hideAction = Runnable { this.hide() }

    interface ControllerVisibilityListener {
        /**
         * Called when the visibility of the controller ui changes.
         *
         * @param visibility new visibility of controller ui. Either [View.VISIBLE] or
         * [View.GONE].
         */
        fun onControllerVisibilityChange(visibility: Int)
    }

    interface FullscreenActionListener {
        /**
         * @return if enter or exit fullscreen action was handled
         */
        fun onFullscreenAction(enterFullscreen: Boolean): Boolean
    }

    init {

        ffwdMs = DEFAULT_FAST_FORWARD_MS
        rewindMs = DEFAULT_REWIND_MS
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS

        uiListener = UiListener()

        LayoutInflater.from(context).inflate(R.layout.include_player_control_view, this)

        playButton = findViewById(R.id.vdo_play)
        playButton.setOnClickListener(uiListener)
        pauseButton = findViewById(R.id.vdo_pause)
        pauseButton.setOnClickListener(uiListener)
        pauseButton.visibility = View.GONE
        fastForwardButton = findViewById(R.id.vdo_ffwd)
        fastForwardButton.setOnClickListener(uiListener)
        rewindButton = findViewById(R.id.vdo_rewind)
        rewindButton.setOnClickListener(uiListener)
        durationView = findViewById(R.id.vdo_duration)
        positionView = findViewById(R.id.vdo_position)
        seekBar = findViewById(R.id.vdo_seekbar)
        seekBar.setOnSeekBarChangeListener(uiListener)
        speedControlButton = findViewById(R.id.vdo_speed)
        speedControlButton.setOnClickListener(uiListener)
        captionsButton = findViewById(R.id.vdo_captions)
        captionsButton.setOnClickListener(uiListener)
        qualityButton = findViewById(R.id.vdo_quality)
        qualityButton.setOnClickListener(uiListener)
        enterFullscreenButton = findViewById(R.id.vdo_enter_fullscreen)
        enterFullscreenButton.setOnClickListener(uiListener)
        exitFullscreenButton = findViewById(R.id.vdo_exit_fullscreen)
        exitFullscreenButton.setOnClickListener(uiListener)
        exitFullscreenButton.visibility = View.GONE
        loaderView = findViewById(R.id.vdo_loader)
        loaderView.visibility = View.GONE
        errorView = findViewById(R.id.vdo_error)
        errorView.setOnClickListener(uiListener)
        errorView.visibility = View.GONE
        errorTextView = findViewById(R.id.vdo_error_text)
        errorTextView.setOnClickListener(uiListener)
        errorTextView.visibility = View.GONE
        controlPanel = findViewById(R.id.vdo_control_panel)
        setOnClickListener(uiListener)
    }

    fun setPlayer(vdoPlayer: VdoPlayer) {
        if (player === vdoPlayer) return

        if (player != null) {
            player!!.removePlaybackEventListener(uiListener)
        }
        player = vdoPlayer
        if (player != null) {
            player!!.addPlaybackEventListener(uiListener)
        }
    }

    fun setFullscreenActionListener(fullscreenActionListener: FullscreenActionListener) {
        this.fullscreenActionListener = fullscreenActionListener
    }

    fun setControllerVisibilityListener(visibilityListener: ControllerVisibilityListener) {
        this.visibilityListener = visibilityListener
    }

    fun show() {
        if (!controllerVisible()) {
            controlPanel.visibility = View.VISIBLE
            updateAll()
            if (visibilityListener != null) {
                visibilityListener!!.onControllerVisibilityChange(controlPanel.visibility)
            }
        }
        hideAfterTimeout()
    }

    fun hide() {
        if (controllerVisible() && lastErrorParams == null) {
            controlPanel.visibility = View.GONE
            removeCallbacks(hideAction)
            if (visibilityListener != null) {
                visibilityListener!!.onControllerVisibilityChange(controlPanel.visibility)
            }
        }
    }

    /**
     * Call if fullscreen in entered/exited in response to external triggers such as orientation
     * change, back button etc.
     * @param fullscreen true if fullscreen in new state
     */
    fun setFullscreenState(fullscreen: Boolean) {
        this.fullscreen = fullscreen
        updateFullscreenButtons()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttachedToWindowOrNot = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttachedToWindowOrNot = false
        removeCallbacks(hideAction)
    }

    /**
     * Call to know the visibility of the playback controls ui. VdoPlayerControlView itself doesn't
     * change visibility when hiding ui controls.
     * @return true if playback controls are visible
     */
    fun controllerVisible(): Boolean {
        return controlPanel.visibility == View.VISIBLE
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        val playing = player != null && player!!.playWhenReady
        if (showTimeoutMs > 0 && isAttachedToWindow && lastErrorParams == null && playing) {
            postDelayed(hideAction, showTimeoutMs.toLong())
        }
    }

    private fun updateAll() {
        updatePlayPauseButtons()
        updateSpeedControlButton()
    }

    private fun updatePlayPauseButtons() {
        if (!controllerVisible() || !isAttachedToWindow) {
            return
        }

        val playbackState = if (player != null) player!!.playbackState else VdoPlayer.STATE_IDLE
        val playing = (player != null
                && playbackState != VdoPlayer.STATE_IDLE && playbackState != VdoPlayer.STATE_ENDED
                && player!!.playWhenReady)
        playButton.visibility = if (playing) View.GONE else View.VISIBLE
        pauseButton.visibility = if (playing) View.VISIBLE else View.GONE
    }

    private fun rewind() {
        if (player != null && rewindMs > 0) {
            player!!.seekTo(Math.max(0, player!!.currentTime - rewindMs))
        }
    }

    private fun fastForward() {
        if (player != null && ffwdMs > 0) {
            player!!.seekTo(Math.min(player!!.duration, player!!.currentTime + ffwdMs))
        }
    }

    private fun updateLoader(loading: Boolean) {
        loaderView.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun updateSpeedControlButton() {
        if (!controllerVisible() || !isAttachedToWindow) {
            return
        }

        if (player != null && player!!.isSpeedControlSupported) {
            speedControlButton.visibility = View.VISIBLE
            val speed = player!!.playbackSpeed
            chosenSpeedIndex = Utils.getClosestFloatIndex(allowedSpeedList, speed)
            speedControlButton.text = allowedSpeedStrList[chosenSpeedIndex]
        } else {
            speedControlButton.visibility = View.GONE
        }
    }

    private fun toggleFullscreen() {
        if (fullscreenActionListener != null) {
            val handled = fullscreenActionListener!!.onFullscreenAction(!fullscreen)
            if (handled) {
                fullscreen = !fullscreen
                updateFullscreenButtons()
            }
        }
    }

    private fun updateFullscreenButtons() {
        if (!controllerVisible() || !isAttachedToWindow) {
            return
        }

        enterFullscreenButton.visibility = if (fullscreen) View.GONE else View.VISIBLE
        exitFullscreenButton.visibility = if (fullscreen) View.VISIBLE else View.GONE
    }

    private fun showSpeedControlDialog() {
        AlertDialog.Builder(context)
            .setSingleChoiceItems(allowedSpeedStrList, chosenSpeedIndex) { dialog, which ->
                if (player != null) {
                    val speed = allowedSpeedList[which]
                    player!!.playbackSpeed = speed
                }
                dialog.dismiss()
            }
            .setTitle("Choose playback speed")
            .show()
    }

    private fun showTrackSelectionDialog(trackType: Int) {
        if (player == null) {
            return
        }

        // get all available tracks of type trackType
        val availableTracks = player!!.availableTracks
        Log.i(TAG, availableTracks.size.toString() + " tracks available")
        val typeTrackList = ArrayList<Track>()
        for (availableTrack in availableTracks) {
            if (availableTrack.type == trackType) {
                typeTrackList.add(availableTrack)
            }
        }

        // get the selected track of type trackType
        val selectedTracks = player!!.selectedTracks
        var selectedTypeTrack: Track? = null
        for (selectedTrack in selectedTracks) {
            if (selectedTrack.type == trackType) {
                selectedTypeTrack = selectedTrack
                break
            }
        }

        // get index of selected type track in "typeTrackList" to indicate selection in dialog
        var selectedIndex = -1
        if (selectedTypeTrack != null) {
            for (i in typeTrackList.indices) {
                if (typeTrackList[i] == selectedTypeTrack) {
                    selectedIndex = i
                    break
                }
            }
        }

        // first, let's convert tracks to array of TrackHolders for better display in dialog
        val trackHolderList = ArrayList<TrackHolder>()
        for (track in typeTrackList) trackHolderList.add(TrackHolder(track))

        // if captions tracks are available, lets add a DISABLE_CAPTIONS track for turning off captions
        if (trackType == Track.TYPE_CAPTIONS && trackHolderList.size > 0) {
            trackHolderList.add(TrackHolder(Track.DISABLE_CAPTIONS))

            // if no captions are selected, indicate DISABLE_CAPTIONS as selected in dialog
            if (selectedIndex < 0) selectedIndex = trackHolderList.size - 1
        } else if (trackType == Track.TYPE_VIDEO) {
            // todo auto option
            if (trackHolderList.size == 1) {
                // just show a default track option
                trackHolderList.clear()
                //trackHolderList.add(TrackHolder.DEFAULT)
            }
        }

        val trackHolders = trackHolderList.toTypedArray()
        Log.i(TAG, "total " + trackHolders.size + ", selected " + selectedIndex)

        // show the type tracks in dialog for selection
        val title = if (trackType == Track.TYPE_CAPTIONS) "CAPTIONS" else "Quality"
        showSelectionDialog(title, trackHolders, selectedIndex)
    }

    private fun showSelectionDialog(
        title: CharSequence,
        trackHolders: Array<TrackHolder>,
        selectedTrackIndex: Int
    ) {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_single_choice, trackHolders
        )
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setSingleChoiceItems(adapter, selectedTrackIndex) { dialog, which ->
                if (player != null) {
                    if (selectedTrackIndex != which) {
                        // set selection
                        val selectedTrack = trackHolders[which].track
                        Log.i(TAG, "selected track index: $which, $selectedTrack")
                        player!!.selectedTracks = arrayOf(selectedTrack)
                    } else {
                        Log.i(TAG, "track selection unchanged")
                    }
                }
                dialog.dismiss()
            }
            .create()
            .show()

    }

    private fun updateErrorView(errorDescription: ErrorDescription?) {
        if (errorDescription != null) {
            updateLoader(false)
            controlPanel.visibility = View.GONE
            errorView.visibility = View.VISIBLE
            errorTextView.visibility = View.VISIBLE
            val errMsg = "An error occurred : " + errorDescription.errorCode + "\nTap to retry"
            errorTextView.text = errMsg
            show()
        } else {
            controlPanel.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            errorTextView.visibility = View.GONE
            show()
        }
    }

    private fun retryAfterError() {
        if (player != null && lastErrorParams != null) {
            errorView.visibility = View.GONE
            errorTextView.visibility = View.GONE
            controlPanel.visibility = View.VISIBLE
            player!!.load(lastErrorParams)
            lastErrorParams = null
        }
    }

    private inner class UiListener : VdoPlayer.PlaybackEventListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            scrubbing = true
            removeCallbacks(hideAction)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            scrubbing = false
            val seekTarget = seekBar.progress
            if (player != null) player!!.seekTo(seekTarget.toLong())
            hideAfterTimeout()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlayPauseButtons()
            updateLoader(playbackState == VdoPlayer.STATE_BUFFERING)
        }

        override fun onClick(v: View) {
            var hideAfterTimeout = true
            if (player != null) {
                if (v === rewindButton) {
                    rewind()
                } else if (v === playButton) {
                    if (player!!.playbackState == VdoPlayer.STATE_ENDED) {
                        player!!.seekTo(0)
                    }
                    player!!.playWhenReady = true
                } else if (v === pauseButton) {
                    hideAfterTimeout = false
                    player!!.playWhenReady = false
                } else if (v === fastForwardButton) {
                    fastForward()
                } else if (v === speedControlButton) {
                    hideAfterTimeout = false
                    showSpeedControlDialog()
                } else if (v === captionsButton) {
                    hideAfterTimeout = false
                    showTrackSelectionDialog(Track.TYPE_CAPTIONS)
                } else if (v === qualityButton) {
                    hideAfterTimeout = false
                    showTrackSelectionDialog(Track.TYPE_VIDEO)
                } else if (v === enterFullscreenButton || v === exitFullscreenButton) {
                    toggleFullscreen()
                } else if (v === errorView || v === errorTextView) {
                    retryAfterError()
                } else if (v === this@VdoPlayerControlView) {
                    hideAfterTimeout = false
                    if (controllerVisible()) {
                        hide()
                    } else {
                        show()
                    }
                }
            }
            if (hideAfterTimeout) {
                hideAfterTimeout()
            }
        }

        override fun onSeekTo(millis: Long) {}

        override fun onProgress(millis: Long) {
            positionView.text = Utils.digitalClockTime(millis.toInt())
            seekBar.progress = millis.toInt()
        }

        override fun onBufferUpdate(bufferTime: Long) {
            seekBar.secondaryProgress = bufferTime.toInt()
        }

        override fun onPlaybackSpeedChanged(speed: Float) {
            updateSpeedControlButton()
        }

        override fun onLoading(vdoInitParams: VdoPlayer.VdoInitParams) {
            updateLoader(true)
            lastErrorParams = null
            updateErrorView(null)
        }

        override fun onLoaded(vdoInitParams: VdoPlayer.VdoInitParams) {
            durationView.text = Utils.digitalClockTime(player!!.duration.toInt()).toString()
            seekBar.max = player!!.duration.toInt()
            updateSpeedControlButton()
        }

        override fun onLoadError(
            vdoParams: VdoPlayer.VdoInitParams,
            errorDescription: ErrorDescription
        ) {
            lastErrorParams = vdoParams
            updateErrorView(errorDescription)
        }

        override fun onMediaEnded(vdoInitParams: VdoPlayer.VdoInitParams) {
            // todo
        }

        override fun onError(
            vdoParams: VdoPlayer.VdoInitParams,
            errorDescription: ErrorDescription
        ) {
            lastErrorParams = vdoParams
            updateErrorView(errorDescription)
        }

        override fun onTracksChanged(availableTracks: Array<Track>, selectedTracks: Array<Track>) {

        }
    }

    /**
     * A helper class that holds a Track instance and overrides [Object.toString] for
     * captions tracks for displaying to user.
     */
    private class TrackHolder internal constructor(internal val track: Track) {

        /**
         * Change this implementation to show track descriptions as per your app's UI requirements.
         */
        override fun toString(): String {
            if (track === Track.DISABLE_CAPTIONS) {
                return "Turn off Captions"
            } else if (track.type == Track.TYPE_VIDEO) {
                return (track.bitrate / 1024).toString() + "kbps (" + dataExpenditurePerHour(track.bitrate) + ")"
            }

            return ""//if (track.type == Track.TYPE_CAPTIONS) track.language else track.toString()
        }

        private fun dataExpenditurePerHour(bitsPerSec: Int): String {
            val bytesPerHour = if (bitsPerSec <= 0) 0 else bitsPerSec * 3600L / 8
            if (bytesPerHour == 0L) {
                return "-"
            } else {
                val megabytesPerHour = bytesPerHour / (1024 * 1024).toFloat()

                if (megabytesPerHour < 1) {
                    return "1 MB per hour"
                } else if (megabytesPerHour < 1000) {
                    return megabytesPerHour.toInt().toString() + " MB per hour"
                } else {
                    val df = DecimalFormat("#.#")
                    df.roundingMode = RoundingMode.CEILING
                    return df.format((megabytesPerHour / 1024).toDouble()) + " GB per hour"
                }

            }
        }

        companion object {
//            internal val DEFAULT: TrackHolder = object : TrackHolder(null) {
//                override fun toString(): String {
//                    return "Default"
//                }
//            }
        }
    }

    companion object {

        private val TAG = "VdoPlayerControlView"

        val DEFAULT_FAST_FORWARD_MS = 10000
        val DEFAULT_REWIND_MS = 10000
        val DEFAULT_SHOW_TIMEOUT_MS = 3000

        private val allowedSpeedList = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
        private val allowedSpeedStrList =
            arrayOf<CharSequence>("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")
    }
}
