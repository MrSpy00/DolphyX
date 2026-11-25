package com.aegissoft.dolphyx.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API 36 / MagicOS-resilient Foreground Service for BLE, NFC, and local server hardware sessions.
 */
class DolphyXHardwareService : Service() {

  private var wakeLock: PowerManager.WakeLock? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    acquireWakeLock()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }
    val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: getString(R.string.service_notification_text)
    val notification = buildNotification(statusText)
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification,
      ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    )
    _isRunning.value = true
    return START_STICKY
  }

  override fun onDestroy() {
    _isRunning.value = false
    releaseWakeLock()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun acquireWakeLock() {
    val powerManager = getSystemService(PowerManager::class.java) ?: return
    val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
    lock.setReferenceCounted(false)
    lock.acquire(WAKE_LOCK_TIMEOUT_MS)
    wakeLock = lock
  }

  private fun releaseWakeLock() {
    val lock = wakeLock ?: return
    try {
      if (lock.isHeld) {
        lock.release()
      }
    } catch (e: RuntimeException) {
      Log.w(TAG, "Failed to release WakeLock: ${e.message}")
    } finally {
      wakeLock = null
    }
  }

  private fun createNotificationChannel() {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      getString(R.string.service_notification_channel),
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = getString(R.string.service_notification_text)
      setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
  }

  private fun buildNotification(statusText: String): Notification {
    val stopIntent = Intent(this, DolphyXHardwareService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      0,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.service_notification_title))
      .setContentText(statusText)
      .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPendingIntent)
      .build()
  }

  companion object {
    private const val TAG = "DolphyXHardwareService"
    private const val CHANNEL_ID = "dolphyx_hardware_channel"
    private const val NOTIFICATION_ID = 1042
    private const val WAKE_LOCK_TAG = "com.aegissoft.dolphyx:hardware_wakelock"
    private val WAKE_LOCK_TIMEOUT_MS: Long = DolphyXConfig.wakeLockTimeoutMs
    private const val ACTION_STOP = "com.aegissoft.dolphyx.action.STOP_HARDWARE_SERVICE"
    private const val EXTRA_STATUS_TEXT = "extra_status_text"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(context: Context, statusText: String = context.getString(R.string.service_notification_text)) {
      val intent = Intent(context, DolphyXHardwareService::class.java).apply {
        putExtra(EXTRA_STATUS_TEXT, statusText)
      }
      ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, DolphyXHardwareService::class.java)
      context.stopService(intent)
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
      val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
      return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createBatteryOptimizationIntent(context: Context): Intent {
      return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
      }
    }
  }
}
