package com.bank;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object: the only class that contains SQL.
 * It performs database operations; it does NOT decide business rules.
 *
 * Every method receives a Connection so the service layer can group several
 * operations into one database transaction (commit / rollback).
 * All queries use PreparedStatement to prevent SQL injection.
 */
public class AccountDAO {

    /** Inserts a new account and returns the generated account id. */
    public int createAccount(Connection con, String customerName, BigDecimal openingBalance)
            throws SQLException {
        String sql = "INSERT INTO accounts (customer_name, balance) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customerName);
            ps.setBigDecimal(2, openingBalance);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Account created but no id was returned");
    }

    /** Returns true if an account with this id exists. */
    public boolean accountExists(Connection con, int accountId) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Adds money to the balance. Returns the number of rows updated (1 on success). */
    public int deposit(Connection con, int accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /**
     * Conditional update: the sufficient-funds check happens INSIDE the database
     * (balance >= ?), so two simultaneous withdrawals cannot both pass on a stale balance.
     * Returns 1 if the withdrawal was applied, 0 if the funds were insufficient.
     */
    public int withdraw(Connection con, int accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance - ? "
                + "WHERE account_id = ? AND balance >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate();
        }
    }

    /** Returns the current balance, or null if the account does not exist. */
    public BigDecimal getBalance(Connection con, int accountId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("balance") : null;
            }
        }
    }

    /** Records a DEPOSIT or WITHDRAW row in the transactions table. */
    public void recordTransaction(Connection con, int accountId, String type, BigDecimal amount)
            throws SQLException {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        }
    }

    /** Returns all transactions for an account, newest first. */
    public List<TransactionRecord> getTransactionHistory(Connection con, int accountId)
            throws SQLException {
        String sql = "SELECT transaction_id, account_id, transaction_type, amount, transaction_time "
                + "FROM transactions WHERE account_id = ? "
                + "ORDER BY transaction_time DESC, transaction_id DESC";
        List<TransactionRecord> history = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new TransactionRecord(
                            rs.getInt("transaction_id"),
                            rs.getInt("account_id"),
                            rs.getString("transaction_type"),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("transaction_time")));
                }
            }
        }
        return history;
    }
}
