package com.maya.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayaApp() }
    }
}

@Composable
fun MayaApp() {
    val context = LocalContext.current

    var input by remember { mutableStateOf("") }
    var torchOn by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Maya is ready") }

    val messages = remember {
        mutableStateListOf(
            "Maya" to "Namaskaram! 💗 I'm Maya. How can I help you?"
        )
    }

    val tts = remember {
        TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                // Malayalam voice availability depends on the installed TTS engine.
            }
        }
    }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    fun speak(text: String) {
        tts.language = Locale("ml", "IN")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_reply")
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty()) return

        messages.add("You" to text)
        input = ""

        val reply =
            "I received your message! Gemini AI connection will be added in the next phase."

        messages.add("Maya" to reply)
        speak(reply)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spoken.isNullOrBlank()) {
            input = spoken
            messages.add("You" to spoken)
            val reply = "I heard you. Gemini connection is coming next."
            messages.add("Maya" to reply)
            speak(reply)
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                try {
                    val manager = context.getSystemService(
                        Context.CAMERA_SERVICE
                    ) as CameraManager

                    val id = manager.cameraIdList.firstOrNull {
                        manager.getCameraCharacteristics(it)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }

                    if (id != null) {
                        manager.setTorchMode(id, true)
                        torchOn = true
                        status = "Flashlight ON"
                    } else {
                        status = "Flashlight unavailable"
                    }
                } catch (e: Exception) {
                    status = "Flashlight error: ${e.message}"
                }
            } else {
                status = "Camera permission denied"
            }
        }

    fun toggleFlashlight() {
        try {
            if (context.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                return
            }

            val manager = context.getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

            val id = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (id != null) {
                torchOn = !torchOn
                manager.setTorchMode(id, torchOn)
                status = if (torchOn) "Flashlight ON" else "Flashlight OFF"
            }
        } catch (e: Exception) {
            status = "Flashlight error: ${e.message}"
        }
    }

    fun changeVolume(up: Boolean) {
        val audio = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE
            else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        status = "Volume adjusted"
    }

    fun openApp(packageName: String) {
        val launch = context.packageManager
            .getLaunchIntentForPackage(packageName)

        if (launch != null) {
            context.startActivity(launch)
        } else {
            status = "App not installed"
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF10101B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "MAYA AI ✨",
                    color = Color(0xFFFFA6D5),
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(status, color = Color.LightGray)

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (message.first == "Maya")
                                        Color(0xFF29233D)
                                    else Color(0xFF193447),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(message.first, color = Color(0xFFFFA6D5))
                            Text(message.second, color = Color.White)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Maya...") },
                        singleLine = true
                    )

                    Button(onClick = { sendMessage() }) {
                        Text("Send")
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val intent = Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                "ml-IN"
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Speak to Maya"
                            )
                        }

                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            status = "Speech recognition unavailable"
                        }
                    }) {
                        Text("🎙 Voice")
                    }

                    Button(onClick = { toggleFlashlight() }) {
                        Text(if (torchOn) "🔦 OFF" else "🔦 ON")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { changeVolume(true) }) {
                        Text("Volume +")
                    }
                    Button(onClick = { changeVolume(false) }) {
                        Text("Volume -")
                    }
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_DISPLAY_SETTINGS)
                        )
                    }) {
                        Text("Brightness")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS)
                        )
                    }) {
                        Text("Wi-Fi")
                    }
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        )
                    }) {
                        Text("Bluetooth")
                    }
                    Button(onClick = {
                        openApp("com.google.android.youtube")
                    }) {
                        Text("YouTube")
                    }
                }
            }
        }
    }
}
