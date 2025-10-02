package com.example.customtasker

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object SoundUtils {

    private var mediaPlayer: MediaPlayer? = null

    fun playSound(
        context: Context,
        soundUri: Uri,
        onError: ((Exception) -> Unit)? = null
    ) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Request audio focus
            val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM) // Use ALARM to bypass silent mode (if allowed)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { /* optional: handle focus changes */ }
                    .build()
            } else null

            val focusGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            if (focusGranted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Set stream volume to max (best effort)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                // Release any existing player
                mediaPlayer?.release()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, soundUri)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e("SoundUtils", "MediaPlayer error: what=$what, extra=$extra")
                        mp.release()
                        mediaPlayer = null
                        onError?.invoke(Exception("Playback error: $what, $extra"))
                        true
                    }
                    prepareAsync()
                }

            } else {
                onError?.invoke(Exception("Audio focus not granted"))
            }

        } catch (e: Exception) {
            Log.e("SoundUtils", "Error playing sound: ${e.message}", e)
            onError?.invoke(e)
        }
    }

    fun checkDoNotDisturbAccess(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else true
    }

    fun requestDoNotDisturbAccess(context: Context) {
        if (!checkDoNotDisturbAccess(context)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            context.startActivity(intent)
        }
    }
}
