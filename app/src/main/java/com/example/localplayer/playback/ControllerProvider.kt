package com.example.localplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

object ControllerProvider {
    fun buildBrowserAsync(
        context: Context,
        onReady: (MediaBrowser) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        browserFuture.addListener(
            {
                try {
                    onReady(browserFuture.get())
                } catch (t: Throwable) {
                    onError(t)
                }
            },
            MoreExecutors.directExecutor()
        )
    }
}