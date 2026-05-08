package com.nfscan.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfscan.data.filepicker.FileType
import com.nfscan.data.filepicker.rememberFilePickerLauncher

@Composable
fun HomeScreen(onFileReady: (bytes: ByteArray, isImage: Boolean) -> Unit) {
    // Source picker dialog state (camera vs. gallery)
    var showSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberFilePickerLauncher(FileType.IMAGE) { bytes ->
        bytes?.let { onFileReady(it, true) }
    }
    val cameraLauncher = rememberFilePickerLauncher(FileType.CAMERA) { bytes ->
        bytes?.let { onFileReady(it, true) }
    }
    val pdfLauncher = rememberFilePickerLauncher(FileType.PDF) { bytes ->
        bytes?.let { onFileReady(it, false) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NFScan") }) },
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
                text  = "Importar Nota Fiscal",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick  = { showSourceDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Foto da Câmera / Galeria")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick  = { pdfLauncher.launch() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Importar PDF")
            }
        }
    }

    // Dialog to choose between camera and gallery
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Selecionar origem") },
            text  = {
                Column {
                    TextButton(
                        onClick = {
                            showSourceDialog = false
                            cameraLauncher.launch()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Câmera") }

                    TextButton(
                        onClick = {
                            showSourceDialog = false
                            galleryLauncher.launch()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Galeria") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancelar") }
            },
        )
    }
}
