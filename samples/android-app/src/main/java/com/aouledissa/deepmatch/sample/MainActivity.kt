package com.aouledissa.deepmatch.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.aouledissa.deepmatch.sample.deeplinks.AppDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.AppDeeplinkProcessor
import com.aouledissa.deepmatch.sample.deeplinks.OpenProfileDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.OpenSeriesDeeplinkParams

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data ?: "app://sample.deepmatch.dev/series/42".toUri()

        setContent {
            DeeplinkResultScreen(uri = uri)
        }
    }
}

@Composable
private fun DeeplinkResultScreen(uri: Uri) {
    var selectedDemo by rememberSaveable { mutableStateOf(detectDemoUri(uri)) }
    val selectedUri = selectedDemo.uri.toUri()
    val result: AppDeeplinkParams? = AppDeeplinkProcessor.match(selectedUri) as? AppDeeplinkParams
    val match = when (result) {
        is OpenProfileDeeplinkParams -> MatchUi(
            title = "Profile Deeplink",
            subtitle = "Matched and parsed successfully",
            accent = Color(0xFF0E7C66),
            properties = listOf(
                "userId" to result.userId,
                "ref" to (result.ref ?: "absent"),
                "fragment" to result.fragment
            )
        )

        is OpenSeriesDeeplinkParams -> MatchUi(
            title = "Series Deeplink",
            subtitle = "Matched and parsed successfully",
            accent = Color(0xFF1F6FEB),
            properties = listOf(
                "seriesId" to result.seriesId.toString(),
                "ref" to (result.ref ?: "absent")
            )
        )

        null -> MatchUi(
            title = "No Match",
            subtitle = "URI did not match any generated spec",
            accent = Color(0xFF8A1C1C),
            properties = listOf("uri" to selectedUri.toString())
        )
    }

    MaterialTheme(colorScheme = sampleColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF7ED),
                                    Color(0xFFEFEFEF),
                                    Color(0xFFFFF7ED)
                                )
                            )
                        )
                        .padding(innerPadding)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DeepMatch Sample",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "YAML-defined deeplink matched through generated AppDeeplinkProcessor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = "Demo URI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoUri.entries.forEach { demo ->
                                UriOptionButton(
                                    label = demo.label,
                                    selected = selectedDemo == demo,
                                    onClick = { selectedDemo = demo }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = match.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusPill(text = match.title, accent = match.accent)
                                }

                                Text(
                                    text = match.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4B5563)
                                )

                                HorizontalDivider(color = Color(0x1A000000))

                                match.properties.forEach { (label, value) ->
                                    PropertyRow(label = label, value = value)
                                }
                            }
                        }

                        Text(
                            text = "Selected URI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = selectedUri.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color(0x33000000),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UriOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusPill(text: String, accent: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = accent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

private fun sampleColorScheme() = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF1D4ED8),
    onSecondary = Color.White,
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827)
)

private data class MatchUi(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val properties: List<Pair<String, String>>
)

private enum class DemoUri(
    val label: String,
    val uri: String
) {
    Profile(
        label = "Profile",
        uri = "app://sample.deepmatch.dev/profile/john123#details"
    ),
    Series(label = "Series", uri = "app://sample.deepmatch.dev/series/42?ref=home"),
    NoMatch(label = "No Match", uri = "app://sample.deepmatch.dev/unknown");
}

private fun detectDemoUri(uri: Uri): DemoUri {
    return when (val params = AppDeeplinkProcessor.match(uri) as? AppDeeplinkParams) {
        is OpenProfileDeeplinkParams -> DemoUri.Profile
        is OpenSeriesDeeplinkParams -> DemoUri.Series
        null -> DemoUri.NoMatch
    }
}
