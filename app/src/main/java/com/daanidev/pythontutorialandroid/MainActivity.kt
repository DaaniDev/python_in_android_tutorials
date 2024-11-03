package com.daanidev.pythontutorialandroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.daanidev.pythontutorialandroid.ui.theme.PythonTutorialAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if(!Python.isStarted()){
            Python.start(AndroidPlatform(this))
        }
        setContent {
            PythonTutorialAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val mContext = LocalContext.current
    val python = Python.getInstance()
    val pythonFile = python.getModule("show_alert")

   Button(onClick = {
       Toast.makeText(mContext,pythonFile.callAttr("show_toast").toString(),Toast.LENGTH_SHORT).show()
   }) {
       Text(text = "Click Me!")
   }
}
