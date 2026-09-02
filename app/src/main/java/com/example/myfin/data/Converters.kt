package com.example.myfin.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String {
        return type?.name ?: TransactionType.EXPENSE.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        if (value.isNullOrBlank()) return TransactionType.EXPENSE
        return try {
            TransactionType.valueOf(value.trim().uppercase())
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromTransferSubtype(subtype: TransferSubtype?): String {
        return subtype?.name ?: TransferSubtype.NONE.name
    }

    @TypeConverter
    fun toTransferSubtype(value: String?): TransferSubtype {
        if (value.isNullOrBlank()) return TransferSubtype.NONE
        return try {
            TransferSubtype.valueOf(value.trim().uppercase())
        } catch (_: Exception) {
            TransferSubtype.NONE
        }
    }
}
