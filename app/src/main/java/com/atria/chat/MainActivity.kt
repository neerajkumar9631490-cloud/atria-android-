package com.atria.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atria.chat.data.AtriaApi
import com.atria.chat.data.AtriaStore
import com.atria.chat.ui.AtriaHome
import com.atria.chat.ui.ChatViewModel

class MainActivity : ComponentActivity() {

    private lateinit var store: AtriaStore
    private val api = AtriaApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AtriaStore(applicationContext)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            val vm: ChatViewModel = viewModel(factory = ChatViewModel.Factory(store, api))
            AtriaHome(vm)
        }
    }

    override fun onStop() {
        try { api.cancel() } catch (_: Exception) { }
        super.onStop()
    }
}
