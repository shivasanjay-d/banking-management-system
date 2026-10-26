# Banking Management System

A menu-driven console banking application built with **Java, JDBC and MySQL**.
It is an educational project that demonstrates a layered architecture, JDBC usage and
database transactions. It is **not** production banking software.

## Features

- Create an account
- Deposit money
- Withdraw money (rejected if funds are insufficient)
- Check balance
- View transaction history

## Architecture

```
USER
  |
  v
Main.java            -> User interface (menu, input/output)
  |
  v
BankingService.java  -> Business logic (validation, transaction management)
  |
  v
AccountDAO.java      -> Database operations (all SQL lives here)
  |
  v
JDBC Driver
  |
  v
MySQL Database       -> accounts, transactions
```

| Class | Responsibility |
|-------|----------------|
| `Main` | Entry point. Shows the menu, reads input, prints results. |
| `BankingService` | Business rules (positive amounts, account exists, sufficient funds) and `commit` / `rollback`. |
| `AccountDAO` | Data Access Object. Runs SQL with `PreparedStatement`. |
| `DBConnection` | Creates JDBC connections; credentials come from env vars or `db.properties`. |
| `TransactionRecord` | Simple object for one row of the `transactions` table. |

## Database

Two tables (see `schema.sql`): `accounts` (id, customer name, balance `DECIMAL(12,2)`) and
`transactions` (id, account id as foreign key, type, amount, timestamp).

## How to run

**Prerequisites:** JDK 17+, Maven, MySQL.

1. Create the database and tables:
   ```bash
   mysql -u root -p < schema.sql
   ```
2. Configure credentials (either option):
   - Copy `db.properties.example` to `db.properties` and edit it (this file is git-ignored), **or**
   - Set environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`.
3. Run:
   ```bash
   mvn compile exec:java
   ```
   Run it from the project root so `db.properties` is found. If the console input
   behaves oddly under Maven, build with `mvn package` and run the classes directly.

## What happens on a deposit (account 1001 has 5000, deposit 1000)

1. User enters account ID and amount (`Main`).
2. `BankingService` checks the amount is greater than zero.
3. It checks the account exists.
4. `UPDATE accounts SET balance = balance + ? WHERE account_id = ?`
5. `INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, 'DEPOSIT', ?)`
6. `commit()`. New balance: 6000.00.

Steps 4 and 5 run in **one database transaction** (`setAutoCommit(false)`). If either fails,
`rollback()` undoes both, so a balance never changes without a matching transaction record.

## What happens on a withdrawal

The funds check is done inside the database with a conditional update:

```sql
UPDATE accounts SET balance = balance - ?
WHERE account_id = ? AND balance >= ?
```

If 0 rows are affected, the balance was insufficient and the withdrawal is rejected. This avoids
relying on a stale balance read earlier, so two simultaneous withdrawals cannot both succeed on
the same old balance.

## Interview notes

**Why separate the code into layers?** `Main` handles the UI, `BankingService` the business rules,
`AccountDAO` the SQL. This makes the code easier to maintain, test and change (for example,
swapping the console UI for a REST API without touching the SQL).

**How does Java talk to MySQL?** Through JDBC: `DriverManager` gives a `Connection`, a
`PreparedStatement` runs parameterised SQL, and a `ResultSet` holds query results.

**Why `PreparedStatement`?** Bound parameters (`?`) help prevent SQL injection.

**Why `DECIMAL` / `BigDecimal` for money?** They store exact decimal values; floating-point types
(`double`, `FLOAT`) can introduce rounding errors.

**What is ACID?** Atomicity (all or nothing), Consistency (rules preserved), Isolation
(concurrent transactions do not interfere incorrectly), Durability (committed data persists).

**`executeQuery()` vs `executeUpdate()`?** `executeQuery()` returns a `ResultSet` (SELECT);
`executeUpdate()` is used for INSERT/UPDATE/DELETE and returns the number of affected rows.

**Primary key vs foreign key?** A primary key uniquely identifies each row. A foreign key links a
row to a row in another table (`transactions.account_id` -> `accounts.account_id`).

## Honest limitations (say these in the interview)

Implemented: layered design, `PreparedStatement`, `DECIMAL`/`BigDecimal`, transactional
deposit/withdraw with rollback, conditional withdrawal update, credentials outside the code.

Not implemented: authentication/authorization, audit logging, unit tests, GUI/REST API,
connection pooling, account-to-account transfers. These are good "how would you improve it"
answers. Only describe features as implemented if you have actually built them.
