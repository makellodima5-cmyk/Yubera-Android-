package com.yubera.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yubera.app.core.ui.theme.YuberaTheme
import com.yubera.app.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YuberaTheme { AppNavHost() } }
    }
}
