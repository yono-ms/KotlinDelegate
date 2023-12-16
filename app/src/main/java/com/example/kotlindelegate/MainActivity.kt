package com.example.kotlindelegate

import android.Manifest
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kotlindelegate.ui.theme.KotlinDelegateTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotlinDelegateTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    Column(Modifier.padding(16.dp)) {
                        Button(onClick = {
                            scope.launch {
                                logger.trace("launch START")
                                runCatching {
                                    getPermission()
                                }.onSuccess {
                                    logger.debug("permission=$it")
                                }.onFailure {
                                    logger.error("getPermission", it)
                                }
                            }
                        }) {
                            Text(text = "get permission")
                        }
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
