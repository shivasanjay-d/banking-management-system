package com.bank;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Business logic layer. Decides WHAT is allowed (validation rules) and
 * manages database transactions (setAutoCommit / commit / rollback).
 * It never contains SQL: that belongs to AccountDAO.
 */
public class BankingService {

    private final AccountDAO dao = new AccountDAO();

    /** Creates an account. Returns the new account id. */
    public int createAccount(String customerName, BigDecimal openingBalance) throws SQLException {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        try (Connection con = DBConnection.getConnection()) {
            return dao.createAccount(con, customerName.trim(), openingBalance);
        }
    }

    /**
     * Deposit: validate input, check the account exists, then update the balance
     * and record the transaction in ONE database transaction.
     * Returns the new balance.
     */
    public BigDecimal deposit(int accountId, BigDecimal amount) throws SQLException {
        requirePositive(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!dao.accountExists(con, accountId)) {
                    throw new IllegalArgumentException("Account " + accountId + " does not exist");
                }
                dao.deposit(con, accountId, amount);
                dao.recordTransaction(con, accountId, "DEPOSIT", amount);
                BigDecimal newBalance = dao.getBalance(con, accountId);
                con.commit();
                return newBalance;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Withdraw: the funds check is done by the database in a conditional UPDATE.
     * If 0 rows are affected the balance was insufficient. The balance change and
     * the transaction record are committed together or rolled back together.
     * Returns the new balance.
     */
    public BigDecimal withdraw(int accountId, BigDecimal amount) throws SQLException {
        requirePositive(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!dao.accountExists(con, accountId)) {
                    throw new IllegalArgumentException("Account " + accountId + " does not exist");
                }
                int rows = dao.withdraw(con, accountId, amount);
                if (rows == 0) {
                    throw new IllegalArgumentException("Insufficient balance");
                }
                dao.recordTransaction(con, accountId, "WITHDRAW", amount);
                BigDecimal newBalance = dao.getBalance(con, accountId);
                con.commit();
                return newBalance;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Returns the current balance of an account. */
    public BigDecimal checkBalance(int accountId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            BigDecimal balance = dao.getBalance(con, accountId);
            if (balance == null) {
                throw new IllegalArgumentException("Account " + accountId + " does not exist");
            }
            return balance;
        }
    }

    /** Returns the transaction history (newest first) of an account. */
    public List<TransactionRecord> getTransactionHistory(int accountId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            if (!dao.accountExists(con, accountId)) {
                throw new IllegalArgumentException("Account " + accountId + " does not exist");
            }
            return dao.getTransactionHistory(con, accountId);
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Invalid amount: must be greater than zero");
        }
    }
}
