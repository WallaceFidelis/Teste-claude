package com.nfscan.ui.scanning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfscan.viewmodel.ReceiptViewModel
import com.nfscan.viewmodel.ScanState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanningScreen(
    filePath: String,
    onScanComplete: () -> Unit,
    onError: () -> Unit,
) {
    val viewModel: ReceiptViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(filePath) {
        viewModel.scan(filePath)
    }

    LaunchedEffect(state) {
        when (state) {
            is ScanState.Success -> onScanComplete()
            is ScanState.Error   -> onError()
            else                 -> Unit
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()

            Spacer(Modifier.height(24.dp))

            val label = when (state) {
                ScanState.ExtractingText -> "Extraindo texto…"
                ScanState.RunningLlm    -> "Analisando com IA…"
                else                    -> "Processando…"
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
