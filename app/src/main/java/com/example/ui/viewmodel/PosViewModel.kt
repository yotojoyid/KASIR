package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.StoreConfig
import com.example.data.local.StorePreferences
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.repository.PosRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncStatus
import com.example.util.CurrencyFormatter
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class CartItem(
    val product: ProductEntity,
    val quantity: Int,
    val discountPercent: Double = 0.0
) {
    val subtotal: Double get() = (product.sellPrice * quantity) * (1.0 - discountPercent / 100.0)
    val totalCost: Double get() = product.buyPrice * quantity
}

enum class ReportPeriod(val title: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua Waktu")
}

data class CompletedOrder(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val storePreferences = StorePreferences(application)
    val repository = PosRepository(database.productDao(), database.transactionDao(), database.syncLogDao())
    val syncManager = CloudSyncManager(repository, storePreferences, viewModelScope)

    // Store Config
    val storeConfig: StateFlow<StoreConfig> = storePreferences.configFlow

    // Products
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { all, query, category ->
        all.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.barcode.contains(query, ignoreCase = true) ||
                    item.sku.contains(query, ignoreCase = true)
            val matchesCategory = category == "Semua" || item.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _customerName = MutableStateFlow("Pelanggan Umum")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _orderDiscount = MutableStateFlow(0.0)
    val orderDiscount: StateFlow<Double> = _orderDiscount.asStateFlow()

    // Active Completed Receipt / Order for Dialog
    private val _lastCompletedOrder = MutableStateFlow<CompletedOrder?>(null)
    val lastCompletedOrder: StateFlow<CompletedOrder?> = _lastCompletedOrder.asStateFlow()

    // Reports
    private val _reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialData()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setOrderDiscount(discount: Double) {
        _orderDiscount.value = discount
    }

    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscount.value = 0.0
        _customerName.value = "Pelanggan Umum"
    }

    suspend fun findProductByBarcode(barcode: String): ProductEntity? {
        val product = repository.getProductByBarcode(barcode.trim())
        if (product != null) {
            addToCart(product)
        }
        return product
    }

    fun saveProduct(product: ProductEntity, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            syncManager.checkUnsyncedItems()
            onComplete()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            syncManager.checkUnsyncedItems()
        }
    }

    fun processCheckout(
        paymentMethod: String,
        paymentDetail: String,
        cashReceived: Double,
        onSuccess: (CompletedOrder) -> Unit
    ) {
        val cart = _cartItems.value
        if (cart.isEmpty()) return

        val config = storeConfig.value
        val subtotal = cart.sumOf { it.subtotal }
        val discount = _orderDiscount.value
        val tax = if (config.isTaxEnabled) (subtotal - discount) * (config.taxPercentage / 100.0) else 0.0
        val total = (subtotal - discount + tax).coerceAtLeast(0.0)
        val totalCost = cart.sumOf { it.totalCost }
        val change = if (paymentMethod == "CASH") (cashReceived - total).coerceAtLeast(0.0) else 0.0

        val invoiceNumber = CurrencyFormatter.generateInvoiceNumber()

        val transaction = TransactionEntity(
            invoiceNumber = invoiceNumber,
            timestamp = System.currentTimeMillis(),
            customerName = _customerName.value.ifBlank { "Pelanggan Umum" },
            cashierName = config.cashierName,
            subtotal = subtotal,
            discountAmount = discount,
            taxAmount = tax,
            totalAmount = total,
            totalCost = totalCost,
            paymentMethod = paymentMethod,
            paymentDetail = paymentDetail,
            cashReceived = if (paymentMethod == "CASH") cashReceived else total,
            changeAmount = change,
            deviceId = config.deviceId,
            isSynced = false
        )

        val items = cart.map {
            TransactionItemEntity(
                transactionId = 0L,
                productId = it.product.id,
                productName = it.product.name,
                barcode = it.product.barcode,
                quantity = it.quantity,
                unitPrice = it.product.sellPrice,
                costPrice = it.product.buyPrice,
                subtotal = it.subtotal
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val txId = repository.checkoutTransaction(transaction, items)
            val completedTx = transaction.copy(id = txId)
            val completedOrder = CompletedOrder(completedTx, items)
            _lastCompletedOrder.value = completedOrder

            clearCart()
            syncManager.checkUnsyncedItems()

            // Trigger auto-sync in background if enabled
            if (config.isAutoSync) {
                syncManager.triggerSync()
            }

            launch(Dispatchers.Main) {
                onSuccess(completedOrder)
            }
        }
    }

    fun dismissCompletedOrder() {
        _lastCompletedOrder.value = null
    }

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun getFilteredTransactions(all: List<TransactionEntity>, period: ReportPeriod): List<TransactionEntity> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                all.filter { it.timestamp in start..now }
            }
            ReportPeriod.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                all.filter { it.timestamp in start..now }
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                all.filter { it.timestamp in start..now }
            }
            ReportPeriod.ALL_TIME -> all
        }
    }

    fun generatePdfReport(context: Context, period: ReportPeriod, onReady: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allTransactions.stateIn(viewModelScope).value
            val filtered = getFilteredTransactions(all, period)
            val itemsMap = mutableMapOf<Long, List<TransactionItemEntity>>()

            for (tx in filtered) {
                itemsMap[tx.id] = repository.getItemsForTransaction(tx.id)
            }

            val totalRevenue = filtered.sumOf { it.totalAmount }
            val totalProfit = filtered.sumOf { it.totalAmount - it.totalCost }

            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            val start = when (period) {
                ReportPeriod.TODAY -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.timeInMillis
                }
                ReportPeriod.LAST_7_DAYS -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis
                }
                ReportPeriod.THIS_MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1); cal.timeInMillis
                }
                ReportPeriod.ALL_TIME -> filtered.minOfOrNull { it.timestamp } ?: 0L
            }

            val reportData = PdfReportGenerator.ReportData(
                periodTitle = period.title,
                startDate = start,
                endDate = end,
                transactions = filtered,
                itemsMap = itemsMap,
                totalRevenue = totalRevenue,
                totalProfit = totalProfit,
                storeConfig = storeConfig.value
            )

            val file = PdfReportGenerator.generateSalesReportPdf(context, reportData)
            launch(Dispatchers.Main) {
                onReady(file)
            }
        }
    }

    fun saveStoreConfig(config: StoreConfig) {
        storePreferences.saveConfig(config)
    }

    fun toggleDesktopMode() {
        val current = storeConfig.value
        val updated = current.copy(isDesktopModePreferred = !current.isDesktopModePreferred)
        storePreferences.saveConfig(updated)
    }

    fun syncNow(onResult: (Boolean, String) -> Unit) {
        syncManager.triggerSync(onResult)
    }
}
