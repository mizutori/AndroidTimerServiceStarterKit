package com.goldrushcomputing.androidtimerservicestarterkit

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*


/**
 * Created by Takamitsu Mizutori on 2019/08/09.
 */
class TimerService: Service() {

    val LOG_TAG = TimerService::class.java.simpleName

    private val binder = TimerServiceBinder()

    private val CHANNEL_ID = "com.goldrushcomputing.androidtimerservicestarterkit.Channel"
    private val NOTIFICATION_ID = 7777


    var timer: Timer? = null
    var startTimeInMillis: Long = 0

    var sencondsToExpire: Long = 30 * 60 //30 minutes
    var remainingTimeInSeconds: Long = sencondsToExpire

    var notificationBuilder: NotificationCompat.Builder? = null

    init {

    }


    override fun onStartCommand(i: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(i, flags, startId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground()
        }

        return Service.START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onRebind(intent: Intent) {
        Log.d(LOG_TAG, "onRebind ")
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(LOG_TAG, "onUnbind ")

        return true
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy ")
    }


    //This is where we detect the app is being killed, thus stop service.
    override fun onTaskRemoved(rootIntent: Intent) {
        Log.d(LOG_TAG, "onTaskRemoved ")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }

    }

    /**
     * Binder class
     *
     * @author Takamitsu Mizutori
     */
    inner class TimerServiceBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }


    private fun startForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(CHANNEL_ID, "Timer Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }


        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            }



        notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Your Timer")
            .setContentText("Remaining: - ")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setTicker("Remaining: - ")


        startForeground(NOTIFICATION_ID, notificationBuilder!!.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun startTimer(seconds: Long){
        this.sencondsToExpire = seconds
        startTimeInMillis = System.currentTimeMillis()


        val task = object : TimerTask() {
            override fun run() {
                calcAndSendCurrentRemainingTime()

            }
        }
        timer = Timer()
        val interval = 20.toLong() //20 msec interval.
        timer?.schedule(task, 0, interval)
    }

    private fun calcAndSendCurrentRemainingTime(){
        val currentTimeInMillis = System.currentTimeMillis()

        val elapsedTimeInMillis = currentTimeInMillis - startTimeInMillis

        val remainingTimeInMillis = sencondsToExpire * 1000 - elapsedTimeInMillis

        remainingTimeInSeconds = remainingTimeInMillis / 1000

        val intent = Intent("TimerUpdated")
        intent.putExtra("remainingTimeInSeconds", remainingTimeInSeconds)
        LocalBroadcastManager.getInstance(this@TimerService.application).sendBroadcast(intent)

        notificationBuilder?.let{

            val minutes = remainingTimeInSeconds / 60
            val seconds = remainingTimeInSeconds % 60

            val notificationText = String.format(getString(R.string.notification_display_format), minutes, seconds)

            it.setContentText(notificationText)
            startForeground(NOTIFICATION_ID, it.build())
        }

    }

    fun stopTimer(){
        timer?.cancel()
        timer = null
    }

}