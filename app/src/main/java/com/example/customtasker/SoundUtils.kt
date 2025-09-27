package com.example.customtasker

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object SoundUtils {
    fun playSound(context: Context, uri: Uri) {
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundUtils", "Failed to play sound: ${e.message}", e)
        }
    }
}