package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SyncLogDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_kilat_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialProducts(database.productDao())
                    }
                }
            }

            private suspend fun populateInitialProducts(productDao: ProductDao) {
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
}
