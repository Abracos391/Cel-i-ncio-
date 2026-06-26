package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainNavigationContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CelViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CelViewModel by viewModels {
        CelViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigationContainer(viewModel = viewModel)
            }
        }
    }
}
