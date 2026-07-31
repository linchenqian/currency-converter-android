package com.jojo.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jojo.currencyconverter.ui.ConverterRoute
import com.jojo.currencyconverter.ui.theme.CurrencyConverterTheme
import com.jojo.currencyconverter.ui.theme.LocalCurrencyColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrencyConverterTheme {
                val colors = LocalCurrencyColors.current
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.appBackground),
                    color = colors.appBackground,
                ) {
                    ConverterRoute()
                }
            }
        }
    }
}
