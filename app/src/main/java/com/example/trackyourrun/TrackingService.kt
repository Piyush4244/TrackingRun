package com.example.trackyourrun

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
import com.example.trackyourrun.other.Constants
import com.example.trackyourrun.other.Constants.ACTION_PAUSE_SERVICE
import com.example.trackyourrun.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.trackyourrun.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.trackyourrun.other.Constants.ACTION_STOP_SERVICE
import com.example.trackyourrun.other.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.trackyourrun.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.trackyourrun.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trackyourrun.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.trackyourrun.other.Constants.NOTIFICATION_ID
import com.example.trackyourrun.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingService: LifecycleService() {

    var isFirstRun=true
    private lateinit var fusedLocationProvideClient:FusedLocationProviderClient

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProvideClient=FusedLocationProviderClient(this)

        isTracking.observe(this, {
            updateLocationTracking(it)
        })
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    val locationCallback=object: LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if(isTracking.value!!){
                result.locations.let { locations->
                    for(location in locations){
                        addPathPoint(location)
                    }
                }
            }
        }
    }

    private fun updateLocationTracking(isTracking:Boolean){
            if(isTracking){
                if(Constants.checkLocationPermission(this)){
                    val request=LocationRequest.create().apply {
                        interval= LOCATION_UPDATE_INTERVAL
                        fastestInterval= FASTEST_LOCATION_UPDATE_INTERVAL
                        priority=LocationRequest.PRIORITY_HIGH_ACCURACY
                    }
                    fusedLocationProvideClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }else{
                fusedLocationProvideClient.removeLocationUpdates(locationCallback)
            }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        Timber.d("Started the service")
                    }else {
                        Timber.d("resumed the  service")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        addEmptyPolyline()
        isTracking.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
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