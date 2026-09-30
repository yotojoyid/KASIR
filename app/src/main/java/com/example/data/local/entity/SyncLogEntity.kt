package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // PUSH, PULL, BACKUP, RESTORE
    val status: String, // SUCCESS, PENDING, FAILED
    val recordCount: Int = 0,
    val details: String = ""
)
