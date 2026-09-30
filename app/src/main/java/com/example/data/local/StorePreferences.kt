package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoreConfig(
    val storeName: String = "KasirKilat Retail",
    val storeAddress: String = "Jl. Merdeka No. 45, Jakarta",
    val storePhone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat dikembalikan.",
    val cashierName: String = "Kasir 01",
    val deviceId: String = "DEV-KASIR-01",
    val storeCloudId: String = "CLOUD-STORE-102",
    val cloudSyncUrl: String = "https://cloud-pos-sync.kasirkilat.id/api",
    val isAutoSync: Boolean = true,
    val taxPercentage: Double = 0.0,
    val isTaxEnabled: Boolean = false,
    val qrisMerchantName: String = "KASIRKILAT STORE",
    val qrisNmid: String = "ID1020304050607",
    val isDesktopModePreferred: Boolean = false
)

class StorePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pos_store_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<StoreConfig> = _configFlow.asStateFlow()

    fun getConfig(): StoreConfig = loadConfig()

    private fun loadConfig(): StoreConfig {
        return StoreConfig(
            storeName = prefs.getString("storeName", "KasirKilat Retail") ?: "KasirKilat Retail",
            storeAddress = prefs.getString("storeAddress", "Jl. Merdeka No. 45, Jakarta") ?: "Jl. Merdeka No. 45, Jakarta",
            storePhone = prefs.getString("storePhone", "0812-3456-7890") ?: "0812-3456-7890",
            receiptFooter = prefs.getString("receiptFooter", "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat dikembalikan.") ?: "Terima Kasih",
            cashierName = prefs.getString("cashierName", "Kasir 01") ?: "Kasir 01",
            deviceId = prefs.getString("deviceId", "DEV-KASIR-01") ?: "DEV-KASIR-01",
            storeCloudId = prefs.getString("storeCloudId", "CLOUD-STORE-102") ?: "CLOUD-STORE-102",
            cloudSyncUrl = prefs.getString("cloudSyncUrl", "https://cloud-pos-sync.kasirkilat.id/api") ?: "https://cloud-pos-sync.kasirkilat.id/api",
            isAutoSync = prefs.getBoolean("isAutoSync", true),
            taxPercentage = prefs.getFloat("taxPercentage", 0.0f).toDouble(),
            isTaxEnabled = prefs.getBoolean("isTaxEnabled", false),
            qrisMerchantName = prefs.getString("qrisMerchantName", "KASIRKILAT STORE") ?: "KASIRKILAT STORE",
            qrisNmid = prefs.getString("qrisNmid", "ID1020304050607") ?: "ID1020304050607",
            isDesktopModePreferred = prefs.getBoolean("isDesktopModePreferred", false)
        )
    }

    fun saveConfig(config: StoreConfig) {
        prefs.edit()
            .putString("storeName", config.storeName)
            .putString("storeAddress", config.storeAddress)
            .putString("storePhone", config.storePhone)
            .putString("receiptFooter", config.receiptFooter)
            .putString("cashierName", config.cashierName)
            .putString("deviceId", config.deviceId)
            .putString("storeCloudId", config.storeCloudId)
            .putString("cloudSyncUrl", config.cloudSyncUrl)
            .putBoolean("isAutoSync", config.isAutoSync)
            .putFloat("taxPercentage", config.taxPercentage.toFloat())
            .putBoolean("isTaxEnabled", config.isTaxEnabled)
            .putString("qrisMerchantName", config.qrisMerchantName)
            .putString("qrisNmid", config.qrisNmid)
            .putBoolean("isDesktopModePreferred", config.isDesktopModePreferred)
            .apply()

        _configFlow.value = config
    }
}
