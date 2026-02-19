package com.example.localplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

object ControllerProvider {
    fun buildControllerAsync(
        context: Context,
        onReady: (MediaController) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    onReady(controllerFuture.get())
                } catch (t: Throwable) {
                    onError(t)
                }
            },
            MoreExecutors.directExecutor()
        )
    }
}