package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.StoreConfig
import com.example.data.local.entity.SyncLogEntity
import com.example.data.sync.SyncState
import com.example.ui.viewmodel.PosViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun CloudSyncScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isSyncingNow by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }

    // Editable Store Configuration State
    var storeName by remember(storeConfig) { mutableStateOf(storeConfig.storeName) }
    var storeAddress by remember(storeConfig) { mutableStateOf(storeConfig.storeAddress) }
    var storePhone by remember(storeConfig) { mutableStateOf(storeConfig.storePhone) }
    var cashierName by remember(storeConfig) { mutableStateOf(storeConfig.cashierName) }
    var deviceId by remember(storeConfig) { mutableStateOf(storeConfig.deviceId) }
    var storeCloudId by remember(storeConfig) { mutableStateOf(storeConfig.storeCloudId) }
    var cloudSyncUrl by remember(storeConfig) { mutableStateOf(storeConfig.cloudSyncUrl) }
    var isAutoSync by remember(storeConfig) { mutableStateOf(storeConfig.isAutoSync) }
    var receiptFooter by remember(storeConfig) { mutableStateOf(storeConfig.receiptFooter) }
    var isTaxEnabled by remember(storeConfig) { mutableStateOf(storeConfig.isTaxEnabled) }
    var taxPercentageText by remember(storeConfig) { mutableStateOf(storeConfig.taxPercentage.toString()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sinkronisasi Cloud",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Akses multi-perangkat & pengaturan kasir",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Cloud Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncStatus.unsyncedCount > 0) Color(0xFFFEF3C7) else Color(0xFFF0FDF4)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (syncStatus.unsyncedCount > 0) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (syncStatus.unsyncedCount > 0) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = if (syncStatus.unsyncedCount > 0) Color(0xFFB45309) else Color(0xFF15803D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (syncStatus.state == SyncState.SYNCING) "Sedang Sinkronisasi..."
                                        else if (syncStatus.unsyncedCount > 0) "${syncStatus.unsyncedCount} Perubahan Belum Diunggah"
                                        else "Cloud Tersinkronisasi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (syncStatus.unsyncedCount > 0) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "Channel: ${storeConfig.storeCloudId} • ${syncStatus.activeDevicesCount} Perangkat Aktif",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terakhir Sinkron:",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = CurrencyFormatter.formatDateTime(syncStatus.lastSyncTime),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isSyncingNow = true
                                viewModel.syncNow { success, msg ->
                                    isSyncingNow = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("sync_now_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (syncStatus.unsyncedCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menghubungkan ke Cloud...")
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sinkronkan Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Multi-Device Sync & Backup Tools
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Akses Multi-Perangkat (JSON Backup)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ekspor katalog dan transaksi untuk disalin ke tablet atau kasir cabang lain secara instan.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        exportJsonText = viewModel.syncManager.exportCloudSyncJson()
                                        showExportDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ekspor Data", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Impor Data", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Store Profile & Cloud Settings Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengaturan Toko & Kasir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Nama Toko / Usaha") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = storeAddress,
                            onValueChange = { storeAddress = it },
                            label = { Text("Alamat Toko") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = storePhone,
                            onValueChange = { storePhone = it },
                            label = { Text("No. Telepon Toko") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cashierName,
                                onValueChange = { cashierName = it },
                                label = { Text("Nama Kasir") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = deviceId,
                                onValueChange = { deviceId = it },
                                label = { Text("ID Perangkat") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = storeCloudId,
                            onValueChange = { storeCloudId = it },
                            label = { Text("Cloud Channel / Store ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = cloudSyncUrl,
                            onValueChange = { cloudSyncUrl = it },
                            label = { Text("Cloud Sync API Gateway") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Auto-Sync Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Sync Real-time",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Sinkronkan setiap ada transaksi baru",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAutoSync,
                                onCheckedChange = { isAutoSync = it }
                            )
                        }

                        // Tax Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Terapkan PPN (%)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Tambahkan PPN ke total tagihan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isTaxEnabled,
                                onCheckedChange = { isTaxEnabled = it }
                            )
                        }

                        if (isTaxEnabled) {
                            OutlinedTextField(
                                value = taxPercentageText,
                                onValueChange = { taxPercentageText = it },
                                label = { Text("Persentase Pajak (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Pesan Catatan Kaki Struk (Footer)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val updatedConfig = storeConfig.copy(
                                    storeName = storeName,
                                    storeAddress = storeAddress,
                                    storePhone = storePhone,
                                    cashierName = cashierName,
                                    deviceId = deviceId,
                                    storeCloudId = storeCloudId,
                                    cloudSyncUrl = cloudSyncUrl,
                                    isAutoSync = isAutoSync,
                                    isTaxEnabled = isTaxEnabled,
                                    taxPercentage = taxPercentageText.toDoubleOrNull() ?: 0.0,
                                    receiptFooter = receiptFooter
                                )
                                viewModel.saveStoreConfig(updatedConfig)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Pengaturan toko berhasil disimpan!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("save_store_settings_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Pengaturan")
                        }
                    }
                }
            }

            // Sync Activity Log
            item {
                Text(
                    text = "Riwayat Aktivitas Cloud (${syncLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (syncLogs.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada riwayat aktivitas sinkronisasi.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(syncLogs, key = { it.id }) { log ->
                    SyncLogItemRow(log)
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Ekspor Data JSON") },
            text = {
                Column {
                    Text("Salin data JSON berikut untuk diimpor pada perangkat kasir lain:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("KasirKilat Sync JSON", exportJsonText)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("JSON disalin ke clipboard!")
                        }
                    }
                ) {
                    Text("Salin ke Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Impor Data JSON Cloud") },
            text = {
                Column {
                    Text("Tempelkan data JSON dari perangkat kasir utama:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Tempel JSON di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.syncManager.importCloudSyncJson(importJsonText)
                            showImportDialog = false
                            snackbarHostState.showSnackbar(
                                if (success) "Data berhasil diimpor & tersinkron!" else "Format JSON tidak valid."
                            )
                        }
                    }
                ) {
                    Text("Impor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SyncLogItemRow(log: SyncLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (log.status == "SUCCESS") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${log.action} - ${log.status}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.status == "SUCCESS") Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = CurrencyFormatter.formatDateTime(log.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = log.details,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
