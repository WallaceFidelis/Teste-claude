package com.nfscan.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onFileSelected: (filePath: String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("NFScan") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Importar Nota Fiscal",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { /* TODO: launch platform image picker → onFileSelected(path) */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Foto da Câmera / Galeria")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* TODO: launch platform PDF picker → onFileSelected(path) */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Importar PDF")
            }
        }
    }
}
