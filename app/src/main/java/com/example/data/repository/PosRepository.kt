package com.example.data.repository

import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SyncLogDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PosRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val syncLogDao: SyncLogDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val syncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getAllLogs()

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts(query)
        }
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun updateProductStock(productId: Long, newStock: Int) {
        productDao.updateStock(productId, newStock)
    }

    suspend fun checkoutTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val txId = transactionDao.insertTransaction(transaction)
        val itemsWithTxId = items.map { it.copy(transactionId = txId) }
        transactionDao.insertTransactionItems(itemsWithTxId)

        // Decrease stock for each sold item
        for (item in items) {
            productDao.decreaseStock(item.productId, item.quantity)
        }
        return txId
    }

    suspend fun getTransactionWithItems(transactionId: Long): TransactionWithItems? {
        val tx = transactionDao.getTransactionById(transactionId) ?: return null
        val items = transactionDao.getItemsForTransaction(transactionId)
        return TransactionWithItems(tx, items)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startDate, endDate)
    }

    suspend fun getTransactionsBetweenList(startDate: Long, endDate: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsBetweenList(startDate, endDate)
    }

    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity> {
        return transactionDao.getItemsForTransaction(transactionId)
    }

    suspend fun getUnsyncedCount(): Pair<Int, Int> {
        val unsyncedProducts = productDao.getUnsyncedProducts().size
        val unsyncedTx = transactionDao.getUnsyncedTransactions().size
        return Pair(unsyncedProducts, unsyncedTx)
    }

    suspend fun addSyncLog(action: String, status: String, count: Int, details: String) {
        syncLogDao.insertLog(
            SyncLogEntity(
                action = action,
                status = status,
                recordCount = count,
                details = details
            )
        )
    }

    suspend fun markAllSynced() {
        val products = productDao.getUnsyncedProducts().map { it.id }
        if (products.isNotEmpty()) {
            productDao.markProductsSynced(products)
        }
        val txs = transactionDao.getUnsyncedTransactions().map { it.id }
        if (txs.isNotEmpty()) {
            transactionDao.markTransactionsSynced(txs)
        }
    }

    suspend fun getAllDataForExport(): Triple<List<ProductEntity>, List<TransactionEntity>, List<TransactionItemEntity>> {
        val products = productDao.getAllProducts().first()
        val txs = transactionDao.getAllTransactions().first()
        val items = transactionDao.getAllTransactionItems()
        return Triple(products, txs, items)
    }

    suspend fun importCloudData(products: List<ProductEntity>, txs: List<TransactionEntity>, items: List<TransactionItemEntity>) {
        if (products.isNotEmpty()) productDao.insertAll(products)
        for (tx in txs) transactionDao.insertTransaction(tx)
        if (items.isNotEmpty()) transactionDao.insertTransactionItems(items)
    }

    suspend fun ensureInitialData() {
        if (productDao.getProductCount() == 0) {
            val initialProducts = listOf(
                ProductEntity(
                    sku = "PRD-001",
                    barcode = "8992753123456",
                    name = "Kopi Susu Gula Aren 250ml",
                    category = "Minuman",
                    buyPrice = 12000.0,
                    sellPrice = 18000.0,
                    stock = 50,
                    unit = "Cup"
                ),
                ProductEntity(
                    sku = "PRD-002",
                    barcode = "8991002103456",
                    name = "Indomie Goreng Spesial 85g",
                    category = "Makanan",
                    buyPrice = 2800.0,
                    sellPrice = 3500.0,
                    stock = 120,
                    unit = "Bks"
                ),
                ProductEntity(
                    sku = "PRD-003",
                    barcode = "8998866200112",
                    name = "Teh Botol Sosro Kotak 250ml",
                    category = "Minuman",
                    buyPrice = 3200.0,
                    sellPrice = 4500.0,
                    stock = 75,
                    unit = "Kotak"
                ),
                ProductEntity(
                    sku = "PRD-004",
                    barcode = "8993417200018",
                    name = "Air Mineral Le Minerale 600ml",
                    category = "Minuman",
                    buyPrice = 2500.0,
                    sellPrice = 3500.0,
                    stock = 90,
                    unit = "Btl"
                ),
                ProductEntity(
                    sku = "PRD-005",
                    barcode = "8996001300055",
                    name = "Minyak Goreng Sania Pouch 2L",
                    category = "Sembako",
                    buyPrice = 32000.0,
                    sellPrice = 38500.0,
                    stock = 30,
                    unit = "Pch"
                ),
                ProductEntity(
                    sku = "PRD-006",
                    barcode = "8991001500010",
                    name = "Beras Premium Ramos 5kg",
                    category = "Sembako",
                    buyPrice = 64000.0,
                    sellPrice = 74000.0,
                    stock = 25,
                    unit = "Sak"
                ),
                ProductEntity(
                    sku = "PRD-007",
                    barcode = "8999999120021",
                    name = "Keripik Singkong Balado 150g",
                    category = "Camilan",
                    buyPrice = 8000.0,
                    sellPrice = 12000.0,
                    stock = 40,
                    unit = "Bks"
                ),
                ProductEntity(
                    sku = "PRD-008",
                    barcode = "8992388100119",
                    name = "Roti Tawar Gandum Kupas",
                    category = "Makanan",
                    buyPrice = 13500.0,
                    sellPrice = 16500.0,
                    stock = 20,
                    unit = "Bks"
                )
            )
            productDao.insertAll(initialProducts)
        }
    }
}
