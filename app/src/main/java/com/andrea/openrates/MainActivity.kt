package com.andrea.openrates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.andrea.openrates.ui.ConverterScreen
import com.andrea.openrates.ui.ConverterViewModel
import com.andrea.openrates.ui.theme.OpenRatesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ConverterViewModel by viewModels {
        ConverterViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenRatesTheme {
                val state by viewModel.state.collectAsState()
                ConverterScreen(
                    state = state,
                    onAmountChange = viewModel::onAmountChange,
                    onFromChange = viewModel::onFromChange,
                    onToChange = viewModel::onToChange,
                    onSwap = viewModel::onSwap,
                    onRefresh = viewModel::refresh,
                    onToggleWatch = viewModel::onToggleWatch,
                )
            }
        }
    }

    // On every return to the app, not just launch: a process can stay alive for days.
    override fun onStart() {
        super.onStart()
        viewModel.refreshIfStale()
    }
}
