package com.example.runkeeper

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runkeeper.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
const val NOTIFICATION_CHANNEL_NAME = "tracking"
const val NOTIFICATION_ID = 1
const val ACTION_SHOW_RUN_FRAGMENT = "ACTION_SHOW_RUN_FRAGMENT"

const val LOCATION_UPDATE_INTERVAL = 5000L
const val FASTEST_LOCATION_INTERVAL = 2000L

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingService : LifecycleService() {

    var isFirstRun = true
    var serviceStopped = false

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeInSeconds = MutableLiveData<Long>()

    companion object {
        val timeInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeInSeconds.postValue(0L)
        timeInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
        })
    }

    private fun stopService() {
        serviceStopped = true
        isFirstRun = true
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        starForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("resuming")
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("paused")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("stopped")
                    stopService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var totalTime = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                lapTime = System.currentTimeMillis() - timeStarted
                timeInMillis.postValue(totalTime + lapTime)
                if (timeInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeInSeconds.postValue(timeInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(50L)
            }
            totalTime += lapTime
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
//        if (!serviceStopped) {
            if (isTracking) {
                if (TrackingUtility.hasLocationPermissions(this)) {
                    val request = LocationRequest().apply {
                        interval = LOCATION_UPDATE_INTERVAL
                        fastestInterval = FASTEST_LOCATION_INTERVAL
                        priority = PRIORITY_HIGH_ACCURACY
                    }
                    fusedLocationProviderClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            } else {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
//        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(res: LocationResult?) {
            super.onLocationResult(res)
            if (isTracking.value!!) {
                res?.locations?.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.d("location ${location.longitude} , ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun starForegroundService() {
        startTimer()
        addEmptyPolyline()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        if (!serviceStopped) {
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.running)
                .setContentTitle("Run Keeper")
                .setContentIntent(getMainActivityPendingIntent())

            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_RUN_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }
}