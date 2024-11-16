package com.daanidev.pythontutorialandroid

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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Typeface
import androidx.core.content.ContextCompat
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
                    WaterMark(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WaterMark(modifier: Modifier) {
    var selectedImageURI by remember { mutableStateOf<Uri?>(null) }
    var selectedWaterMarkedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val mContext = LocalContext.current

    val fontFile = getFontByteArray(mContext,R.font.play_fair_display_bold)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageURI = uri
        uri?.let {
            val inputStream: InputStream? = mContext.contentResolver.openInputStream(it)
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream?.copyTo(byteArrayOutputStream)
            inputStream?.close()

            val imageBytes = byteArrayOutputStream.toByteArray()

            // Call Python code with the byte array
            val py = Python.getInstance()
            val pyFile = py.getModule("water_mark")
            val resultPath = mContext.filesDir.path + "/watermarked_image.jpg"

            // For Opacity value will be in range 0 - 255
            pyFile.callAttr("add_text_watermark_from_bytes", imageBytes, resultPath, "Watermark Text",fontFile,"maroon",40,WaterMarkPosition.BOTTOM_CENTER.name,200)

            // Load the result image
            val resultBitmap = BitmapFactory.decodeFile(resultPath)
            selectedWaterMarkedBitmap = resultBitmap
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Select Image")
        }
        Spacer(modifier = Modifier.padding(10.dp))

        selectedImageURI?.let {
            val inputStream = mContext.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }

        }

        selectedWaterMarkedBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )

        }
    }
}

fun getFontByteArray(mContext: Context,fontId: Int): ByteArray {
    val inputStream = mContext.resources.openRawResource(fontId)
    val byteArrayOutputStream = ByteArrayOutputStream()

    var byte: Int
    while (inputStream.read().also { byte = it } != -1) {
        byteArrayOutputStream.write(byte)
    }

    inputStream.close()
    return byteArrayOutputStream.toByteArray()
}

enum class WaterMarkPosition{
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_CENTER,
    BOTTOM_CENTER,
    CENTER
}