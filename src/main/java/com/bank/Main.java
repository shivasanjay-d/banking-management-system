package com.bank;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point and user interface (menu-driven console application).
 * It only reads input and prints output; business rules live in BankingService.
 */
public class Main {

    public static void main(String[] args) {
        BankingService service = new BankingService();
        Scanner sc = new Scanner(System.in);

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1": {
                        System.out.print("Enter customer name: ");
                        String name = sc.nextLine();
                        System.out.print("Enter opening balance: ");
                        BigDecimal opening = readAmount(sc);
                        int id = service.createAccount(name, opening);
                        System.out.println("Account created. Account ID: " + id);
                        break;
                    }
                    case "2": {
                        int id = readAccountId(sc);
                        System.out.print("Enter deposit amount: ");
                        BigDecimal amount = readAmount(sc);
                        BigDecimal newBalance = service.deposit(id, amount);
                        System.out.println("Deposit successful.");
                        System.out.println("New balance: " + newBalance.toPlainString());
                        break;
                    }
                    case "3": {
                        int id = readAccountId(sc);
                        System.out.print("Enter withdrawal amount: ");
                        BigDecimal amount = readAmount(sc);
                        BigDecimal newBalance = service.withdraw(id, amount);
                        System.out.println("Withdrawal successful.");
                        System.out.println("New balance: " + newBalance.toPlainString());
                        break;
                    }
                    case "4": {
                        int id = readAccountId(sc);
                        System.out.println("Balance: " + service.checkBalance(id).toPlainString());
                        break;
                    }
                    case "5": {
                        int id = readAccountId(sc);
                        List<TransactionRecord> history = service.getTransactionHistory(id);
                        if (history.isEmpty()) {
                            System.out.println("No transactions found.");
                        } else {
                            history.forEach(System.out::println);
                        }
                        break;
                    }
                    case "6":
                        System.out.println("Thank you for using the Banking System.");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter 1-6.");
                }
            } catch (IllegalArgumentException e) {
                // Validation / business-rule failures: safe to show to the user.
                System.out.println("Error: " + e.getMessage());
            } catch (SQLException e) {
                // Do not expose database internals to the user.
                System.out.println("A database error occurred. Please try again later.");
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("===== BANKING SYSTEM =====");
        System.out.println();
        System.out.println("1. Create Account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Check Balance");
        System.out.println("5. Transaction History");
        System.out.println("6. Exit");
        System.out.println();
        System.out.print("Enter your choice: ");
    }

    private static int readAccountId(Scanner sc) {
        System.out.print("Enter account ID: ");
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Account ID must be a number");
        }
    }

    private static BigDecimal readAmount(Scanner sc) {
        try {
            return new BigDecimal(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number");
        }
    }
}
