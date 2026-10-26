package com.bank;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Simple value object representing one row of the transactions table. */
public class TransactionRecord {

    private final int transactionId;
    private final int accountId;
    private final String type;
    private final BigDecimal amount;
    private final Timestamp time;

    public TransactionRecord(int transactionId, int accountId, String type,
                             BigDecimal amount, Timestamp time) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.time = time;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Timestamp getTime() {
        return time;
    }

    @Override
    public String toString() {
        return String.format("#%d | %-8s | %12s | %s", transactionId, type, amount.toPlainString(), time);
    }
}
