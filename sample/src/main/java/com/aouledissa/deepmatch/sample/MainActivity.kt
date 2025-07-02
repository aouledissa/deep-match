package com.aouledissa.deepmatch.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aouledissa.deepmatch.processor.DeeplinkProcessor
import com.aouledissa.deepmatch.sample.deeplinks.OpenMoviesDeeplinkSpecs
import com.aouledissa.deepmatch.sample.deeplinks.OpenSeriesDeeplinkSpecs
import com.aouledissa.deepmatch.sample.ui.theme.SampleTheme

class MainActivity : ComponentActivity() {
    val processor = DeeplinkProcessor.Builder()
        .register(
            OpenSeriesDeeplinkSpecs,
            OpenSeriesDeeplinkHandler
        )
        .register(
            OpenMoviesDeeplinkSpecs,
            OpenMoviesDeeplinkHandler
        )
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.data?.let {
            processor.match(deeplink = it, activity = this)
        }
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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
    SampleTheme {
        Greeting("Android")
    }
}