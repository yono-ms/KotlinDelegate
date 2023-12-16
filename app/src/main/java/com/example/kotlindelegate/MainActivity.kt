package com.example.kotlindelegate

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.kotlindelegate.ui.theme.KotlinDelegateTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.resumeWithException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            KotlinDelegateTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val location = remember { mutableStateOf("") }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(onClick = {
                            scope.launch {
                                logger.trace("launch START")
                                runCatching {
                                    val permission = getPermission()
                                    if (permission) {
                                        getLocation()
                                    } else {
                                        throw Error("denied.")
                                    }
                                }.onSuccess {
                                    location.value = "${it.latitude},${it.longitude}"
                                }.onFailure {
                                    logger.error("getPermission", it)
                                    location.value = "${it.message}"
                                }
                            }
                        }) {
                            Text(text = "get location")
                        }
                        Text(text = location.value)
                    }
                }
            }
        }
    }

    private lateinit var permissionContinuation: CancellableContinuation<Boolean>

    @OptIn(ExperimentalCoroutinesApi::class)
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            when {
                map.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    logger.trace("ACCESS_FINE_LOCATION")
                    permissionContinuation.resume(true) {
                        logger.warn("onCancellation", it)
                    }
                }

                map.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    logger.trace("ACCESS_COARSE_LOCATION")
                    permissionContinuation.resume(true) {
                        logger.warn("onCancellation", it)
                    }
                }

                else -> {
                    logger.trace("unknown")
                    permissionContinuation.resume(false) {
                        logger.warn("onCancellation", it)
                    }
                }
            }
        }

    private suspend fun getPermission() = suspendCancellableCoroutine { continuation ->
        logger.trace("suspendCancellableCoroutine START")
        this.permissionContinuation = continuation
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        logger.trace("suspendCancellableCoroutine END")
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getLocation() = suspendCancellableCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KotlinDelegateTheme {
        Greeting("Android")
    }
}

val logger: Logger by lazy { LoggerFactory.getLogger("KotlinDelegate") }
