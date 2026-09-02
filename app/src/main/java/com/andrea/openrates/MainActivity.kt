package com.andrea.openrates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrea.openrates.ui.ConverterScreen
import com.andrea.openrates.ui.ConverterViewModel
import com.andrea.openrates.ui.theme.OpenRatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenRatesTheme {
                val viewModel: ConverterViewModel = viewModel(
                    factory = ConverterViewModel.factory(application),
                )
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
}
