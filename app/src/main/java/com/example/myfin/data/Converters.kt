package com.example.myfin.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String {
        return type?.name ?: TransactionType.EXPENSE.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return try {
            if (value != null) TransactionType.valueOf(value) else TransactionType.EXPENSE
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
        return try {
            if (value != null) TransferSubtype.valueOf(value) else TransferSubtype.NONE
        } catch (_: Exception) {
            TransferSubtype.NONE
        }
    }
}
