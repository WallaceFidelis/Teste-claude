package com.nfscan.ui.scanning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfscan.viewmodel.ReceiptViewModel
import com.nfscan.viewmodel.ScanState

@Composable
fun ScanningScreen(
    viewModel: ReceiptViewModel,
    onScanComplete: () -> Unit,
    onError: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.scan()
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

            Text(
                text  = when (state) {
                    ScanState.ExtractingText -> "Extraindo texto…"
                    ScanState.RunningLlm     -> "Analisando com IA…"
                    else                     -> "Processando…"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
