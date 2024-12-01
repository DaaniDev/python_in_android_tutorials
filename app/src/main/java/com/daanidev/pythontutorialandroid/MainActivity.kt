package com.daanidev.pythontutorialandroid

import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.daanidev.pythontutorialandroid.ui.theme.PythonTutorialAndroidTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.Typeface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            PythonTutorialAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SelectImageScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SelectImageScreen(modifier: Modifier) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var secretMessage by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decryption by remember { mutableStateOf(false) }
     val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    if(decryption.not()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize()
            ) {
                Button(modifier = Modifier.weight(1f), onClick = { launcher.launch("image/*") }) {
                    Text("Encryption")
                }

                Button(modifier = Modifier.weight(1f), onClick = {
                    decryption = !decryption
                }) {
                    Text("Decryption")
                }

            }

            selectedImageUri?.let { uri ->
                Text("Selected Image: $uri")

                TextField(
                    value = secretMessage,
                    onValueChange = { secretMessage = it },
                    label = { Text("Enter Secret Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Enter Secret Key") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    scope.launch {
                        val imageBytes = getBytesFromUri(context, uri)
                        if (imageBytes != null) {
                            val python = Python.getInstance()
                            val steganography = python.getModule("steganography")

                            val result = steganography.callAttr(
                                "hide_text_with_key_bytearray",
                                imageBytes,
                                secretMessage,
                                secretKey
                            ).toJava(ByteArray::class.java)
                             saveImageToMediaStore(context, "encrypted_image.png", result)
                        } else {
                            Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }) {
                    Text("Hide Message")
                }
            }
        }
    }

    if (decryption) {
        DecryptImageScreen()
    }
}

@Composable
fun DecryptImageScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var enteredKey by remember { mutableStateOf("") }
    var extractedMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Select Image from Gallery")
        }

        selectedImageUri?.let { uri ->
            Text("Selected Image: $uri")

            TextField(
                value = enteredKey,
                onValueChange = { enteredKey = it },
                label = { Text("Enter Secret Key") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                scope.launch {
                    val imageBytes = getBytesFromUri(context, uri)
                    if (imageBytes != null) {
                        val python = Python.getInstance()
                        val stego = python.getModule("steganography")

                        val result = stego.callAttr(
                            "extract_text_with_key_bytearray",
                            imageBytes
                        ).asList()

                        Log.i("result_image","$result")

                        extractedMessage = if (result.size == 2) {
                            val extractedKey = result[0].toString()
                            val message = result[1].toString()
                            Log.i("result_image", "$extractedKey / $message")
                            if (extractedKey == enteredKey) {
                                message
                            } else {
                                "Incorrect key!"
                            }
                        } else {
                            "No hidden message found!"
                        }
                    } else {
                        Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Decrypt Message")
            }

            extractedMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


suspend fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }
}

fun saveImageToMediaStore(context: Context, fileName: String, imageBytes: ByteArray) {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Save in Pictures folder
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it).use { outputStream ->
            outputStream?.write(imageBytes)
        }
        Log.d("Gallery", "Image saved to MediaStore: $uri")
        Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
    } ?: run {
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}
