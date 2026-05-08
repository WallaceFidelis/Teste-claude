package com.nfscan.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfscan.data.llm.ParsedReceipt
import com.nfscan.viewmodel.ReceiptViewModel
import com.nfscan.viewmodel.ScanState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResultScreen(onScanAgain: () -> Unit) {
    val viewModel: ReceiptViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    val receipt = (state as? ScanState.Success)?.receipt

    Scaffold(
        topBar = { TopAppBar(title = { Text("Resultado") }) },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        viewModel.reset()
                        onScanAgain()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text("Nova Nota")
                }
            }
        }
    ) { padding ->
        if (receipt == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ReceiptHeader(receipt) }
            items(receipt.items) { item ->
                ReceiptItemRow(
                    name = item.name,
                    qty = item.quantity,
                    unitPrice = item.unitPrice,
                    total = item.totalPrice,
                )
            }
            item {
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text("R$ %.2f".format(receipt.total), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ReceiptHeader(receipt: ParsedReceipt) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(receipt.supermarket, style = MaterialTheme.typography.headlineSmall)
        Text(receipt.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ReceiptItemRow(name: String, qty: Double, unitPrice: Double, total: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text("${qty}x  R$ %.2f".format(unitPrice), style = MaterialTheme.typography.bodySmall)
        }
        Text("R$ %.2f".format(total), style = MaterialTheme.typography.bodyLarge)
    }
}
