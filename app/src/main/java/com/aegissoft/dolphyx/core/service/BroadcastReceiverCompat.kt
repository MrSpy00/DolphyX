package com.aegissoft.dolphyx.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Compatibility helper for registering non-exported dynamic BroadcastReceivers on Android 13+ / API 36.
 */
object BroadcastReceiverCompat {
  fun registerNotExported(
    context: Context,
    receiver: BroadcastReceiver,
    filter: IntentFilter
  ) {
    ContextCompat.registerReceiver(
      context,
      receiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED
    )
  }
}
