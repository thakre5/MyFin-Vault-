package com.example.myfin.data

enum class TransactionType {
    EXPENSE,
    INCOME,
    ASSET,
    TRANSFER
}

enum class TransferSubtype {
    NONE,
    BILL_FUNDING,
    WEALTH_ALLOCATION,
    REBALANCE,
    CASH_WITHDRAWAL
}
