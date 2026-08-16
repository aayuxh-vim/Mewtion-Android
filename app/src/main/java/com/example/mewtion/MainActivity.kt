package com.example.mewtion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the background service immediately
        val serviceIntent = Intent(this, SensorBridgeService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Mewtion Background Bridge Active")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("You can now minimize the app or turn off the screen. Streaming data over USB via port 8765.")
                    }
                }
            }
        }
    }
}