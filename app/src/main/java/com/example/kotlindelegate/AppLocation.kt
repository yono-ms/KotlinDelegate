package com.example.kotlindelegate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

class AppLocation(val getContext: () -> Context) {

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        val context = getContext()
        LocationServices.getFusedLocationProviderClient(context)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getLocation() = suspendCancellableCoroutine { continuation ->
        val context = getContext()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Error("permission denied.")
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            logger.debug("location={}", location)
            if (location == null) {
                continuation.resumeWithException(Error("location is null"))
            } else {
                continuation.resume(location) {
                    logger.warn("onCancellation")
                }
            }
        }
    }
}
