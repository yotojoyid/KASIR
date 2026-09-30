package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreConfig
import com.example.util.BarcodeUtil
import com.example.util.CurrencyFormatter
import com.example.util.QrCodeView
import kotlinx.coroutines.delay

enum class PaymentTab(val title: String, val icon: ImageVector) {
    CASH("Tunai", Icons.Default.Money),
    QRIS("QRIS Digital", Icons.Default.QrCode),
    EWALLET("E-Wallet", Icons.Default.Send),
    BANK("Transfer Bank", Icons.Default.AccountBalance),
    CARD("Kartu EDC", Icons.Default.CreditCard)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentSheet(
    totalAmount: Double,
    storeConfig: StoreConfig,
    onDismiss: () -> Unit,
    onCompletePayment: (method: String, detail: String, cashAmount: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(PaymentTab.CASH) }

    // Cash State
    var cashTenderedText by remember { mutableStateOf(String.format("%.0f", totalAmount)) }
    val cashTendered = cashTenderedText.toDoubleOrNull() ?: 0.0
    val change = (cashTendered - totalAmount).coerceAtLeast(0.0)
    val isCashSufficient = cashTendered >= totalAmount

    // QRIS State
    var isCheckingQris by remember { mutableStateOf(false) }
    var qrisSecondsRemaining by remember { mutableIntStateOf(300) } // 5 minutes

    LaunchedEffect(selectedTab) {
        if (selectedTab == PaymentTab.QRIS) {
            while (qrisSecondsRemaining > 0) {
                delay(1000)
                qrisSecondsRemaining--
            }
        }
    }

    // E-Wallet State
    var selectedEWallet by remember { mutableStateOf("GoPay") }
    var eWalletPhone by remember { mutableStateOf("") }
    var isEWalletVerifying by remember { mutableStateOf(false) }

    // Bank Transfer State
    var selectedBank by remember { mutableStateOf("BCA") }
    val virtualAccountNumber = remember(selectedBank) {
        val bankCode = when (selectedBank) {
            "BCA" -> "88012"
            "Mandiri" -> "89508"
            "BRI" -> "12800"
            else -> "99000"
        }
        val randomSuffix = (10000000..99999999).random()
        "$bankCode$randomSuffix"
    }

    // Card EDC State
    var approvalCode by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Tagihan",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "POS Checkout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB 1: TUNAI (CASH)
            AnimatedVisibility(visible = selectedTab == PaymentTab.CASH) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cashTenderedText,
                        onValueChange = { cashTenderedText = it.filter { char -> char.isDigit() } },
                        label = { Text("Uang Diterima (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_input_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Cash Chips
                    val quickPresets = listOf(
                        "Uang Pas" to totalAmount,
                        "+10rb" to (totalAmount + 10000.0),
                        "20rb" to 20000.0,
                        "50rb" to 50000.0,
                        "100rb" to 100000.0,
                        "200rb" to 200000.0
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPresets.forEach { (label, value) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        cashTenderedText = String.format("%.0f", value)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Kembalian Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCashSufficient) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCashSufficient) "Kembalian:" else "Uang Kurang:",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCashSufficient) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(if (isCashSufficient) change else (totalAmount - cashTendered)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCashSufficient) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isCashSufficient) {
                                onCompletePayment("CASH", "Tunai Pas", cashTendered)
                            }
                        },
                        enabled = isCashSufficient,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_cash_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Selesaikan Pembayaran Tunai", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TAB 2: QRIS DIGITAL
            AnimatedVisibility(visible = selectedTab == PaymentTab.QRIS) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val qrisPayload = remember(totalAmount) {
                        BarcodeUtil.createQrisPayload(
                            merchantName = storeConfig.qrisMerchantName,
                            nmid = storeConfig.qrisNmid,
                            amount = totalAmount,
                            invoiceNumber = CurrencyFormatter.generateInvoiceNumber()
                        )
                    }

                    // Authentic QRIS Stand Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // QRIS Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QRIS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFFE11D48)
                                )
                                Text(
                                    text = "PEMBAYARAN DIGITAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }

                            Text(
                                text = storeConfig.qrisMerchantName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "NMID: ${storeConfig.qrisNmid}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dynamic QR Code Rendering
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QrCodeView(
                                    content = qrisPayload,
                                    modifier = Modifier.size(184.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val minutes = qrisSecondsRemaining / 60
                            val seconds = qrisSecondsRemaining % 60
                            Text(
                                text = "Berlaku hingga: %02d:%02d".format(minutes, seconds),
                                fontSize = 11.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Mendukung GoPay, OVO, DANA, BCA, Mandiri, ShopeePay, LinkAja",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // QRIS Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isCheckingQris = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCheckingQris) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mengecek...", fontSize = 12.sp)
                            } else {
                                Text("Cek Status QRIS", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onCompletePayment(
                                    "QRIS",
                                    "QRIS Berhasil (Ref: QRIS-${System.currentTimeMillis().toString().takeLast(6)})",
                                    totalAmount
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("qris_success_simulation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulasi Bayar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // TAB 3: E-WALLET
            AnimatedVisibility(visible = selectedTab == PaymentTab.EWALLET) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih Penyedia E-Wallet:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val eWallets = listOf("GoPay", "OVO", "DANA", "ShopeePay")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        eWallets.forEach { wallet ->
                            val isSelected = selectedEWallet == wallet
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedEWallet = wallet }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = wallet,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = eWalletPhone,
                        onValueChange = { eWalletPhone = it },
                        label = { Text("Nomor HP Pelanggan") },
                        placeholder = { Text("Contoh: 081234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isEWalletVerifying = true
                            onCompletePayment(
                                "EWALLET",
                                "$selectedEWallet (${if (eWalletPhone.isNotBlank()) eWalletPhone else "0812xxxx"}) - Ref: EW-${System.currentTimeMillis().toString().takeLast(6)}",
                                totalAmount
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ewallet_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Pembayaran $selectedEWallet")
                    }
                }
            }

            // TAB 4: BANK TRANSFER
            AnimatedVisibility(visible = selectedTab == PaymentTab.BANK) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih Bank Tujuan:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val banks = listOf("BCA", "Mandiri", "BRI", "BNI")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        banks.forEach { bank ->
                            val isSelected = selectedBank == bank
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedBank = bank }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bank,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Nomor Virtual Account ($selectedBank):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = virtualAccountNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Atas Nama: ${storeConfig.storeName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onCompletePayment(
                                "BANK_TRANSFER",
                                "$selectedBank VA: $virtualAccountNumber",
                                totalAmount
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifikasi Transfer Sukses")
                    }
                }
            }

            // TAB 5: EDC CARD
            AnimatedVisibility(visible = selectedTab == PaymentTab.CARD) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = approvalCode,
                        onValueChange = { approvalCode = it },
                        label = { Text("Approval Code / Ref EDC") },
                        placeholder = { Text("Contoh: APPR-998234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onCompletePayment(
                                "DEBIT",
                                "Debit/Credit EDC: ${approvalCode.ifBlank { "AUTH-OK" }}",
                                totalAmount
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Transaksi Kartu")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
