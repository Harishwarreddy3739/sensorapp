package com.example.sensorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.sensorapp.ui.CustomComposableShell
import com.example.sensorapp.ui.ResponsiveLayout
import com.example.sensorapp.ui.theme.SensorappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorappTheme {
                // Use the custom shell or responsive layout based on your needs
                Surface(color = MaterialTheme.colorScheme.background) {
                    ResponsiveLayout()
                    CustomComposableShell()
                }
            }
        }
    }
}