package io.github.hamzatadlaoui.socialgraph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.hamzatadlaoui.socialgraph.ui.SocialGraphApp
import io.github.hamzatadlaoui.socialgraph.ui.theme.SocialGraphTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)

        setContent {
            SocialGraphTheme {
                SocialGraphApp(container)
            }
        }
    }
}
