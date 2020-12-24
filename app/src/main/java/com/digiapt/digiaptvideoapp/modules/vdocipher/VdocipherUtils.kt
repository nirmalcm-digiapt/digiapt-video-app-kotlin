package com.digiapt.digiaptvideoapp.modules.vdocipher

import android.util.Log
import android.util.Pair

import com.vdocipher.aegis.player.VdoPlayer

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class
 */

object Utils {
    private val TAG = "Utils"

    // call on non-ui thread only
    val sampleOtpAndPlaybackInfo: Pair<String, String>
        @Throws(IOException::class, JSONException::class)
        get() {
            val SAMPLE_OTP_PLAYBACK_INFO_URL = "https://dev.vdocipher.com/api/videos/d4439f84c375f768a459925337d0114d/otp"

            val url = URL(SAMPLE_OTP_PLAYBACK_INFO_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization","Apisecret yOaqfEa5H6AUQXBhuaFuBy6S5yvquHkA9jvV2OkyMVKrobM2Gk0Ru6JPxxjH1dtG")
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type","application/json")
            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val `is` = connection.inputStream

                val br = BufferedReader(InputStreamReader(`is`))
                var inLine: String
                val responseBuffer = StringBuffer()

                while ((br.readLine()) != null) {
                    responseBuffer.append(br.readLine())
                }
                br.close()

                val response = responseBuffer.toString()
                Log.i(TAG, "response: $response")

                val jObj = JSONObject(response)
                val otp = jObj.getString("otp")
                val playbackInfo = jObj.getString("playbackInfo")
                return Pair.create(otp, playbackInfo)
            } else {
                Log.e(TAG, "error response code = $responseCode")
                throw IOException("Network error, code $responseCode")
            }
        }

    internal fun digitalClockTime(timeInMilliSeconds: Int): String {
        val totalSeconds = timeInMilliSeconds / 1000
        val hours = totalSeconds / (60 * 60)
        val minutes = (totalSeconds - hours * 60 * 60) / 60
        val seconds = totalSeconds - hours * 60 * 60 - minutes * 60

        var timeThumb = ""
        if (hours > 0) {
            if (hours < 10) {
                timeThumb += "0$hours:"
            } else {
                timeThumb += "$hours:"
            }
        }
        if (minutes > 0) {
            if (minutes < 10) {
                timeThumb += "0$minutes:"
            } else {
                timeThumb += "$minutes:"
            }
        } else {
            timeThumb += "00" + ":"
        }
        if (seconds < 10) {
            timeThumb += "0$seconds"
        } else {
            timeThumb += seconds
        }
        return timeThumb
    }

    /**
     * @return index of number in provided array closest to the provided number
     */
    fun getClosestFloatIndex(refArray: FloatArray, comp: Float): Int {
        var distance = Math.abs(refArray[0] - comp)
        var index = 0
        for (i in 1 until refArray.size) {
            val currDistance = Math.abs(refArray[i] - comp)
            if (currDistance < distance) {
                index = i
                distance = currDistance
            }
        }
        return index
    }

    fun playbackStateString(playWhenReady: Boolean, playbackState: Int): String {
        val stateName: String
        when (playbackState) {
            VdoPlayer.STATE_IDLE -> stateName = "STATE_IDLE"
            VdoPlayer.STATE_READY -> stateName = "STATE_READY"
            VdoPlayer.STATE_BUFFERING -> stateName = "STATE_BUFFERING"
            VdoPlayer.STATE_ENDED -> stateName = "STATE_ENDED"
            else -> stateName = "STATE_UNKNOWN"
        }
        return "playWhenReady " + (if (playWhenReady) "true" else "false") + ", " + stateName
    }

    fun getSizeString(bitsPerSec: Int, millisec: Long): String {
        val sizeMB = bitsPerSec.toDouble() / (8 * 1024 * 1024) * (millisec / 1000)
        return round(sizeMB, 2).toString() + " MB"
    }

    fun getSizeBytes(bitsPerSec: Int, millisec: Long): Long {
        return bitsPerSec / 8 * (millisec / 1000)
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)

        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }
}
