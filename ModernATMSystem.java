import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ATM Simulation System
 * Created by: Rahul Verma
 * Date: 17-Apr-2025
 *
 * A comprehensive ATM application with console and GUI interfaces
 * Built for SBI Bank as a prototype system
 *
 * Note: This is using modern Java features like text blocks and records
 * Requires Java 21 or higher to run
 */
public class ModernATMSystem {
    public static void main(String[] args) {
        // Using switch expressions introduced in Java 14
        var mode = args.length > 0 ? args[0].toLowerCase() : "swing";

        switch (mode) {
            case "console" -> {
                // Start console mode
                var atm = new ATM();
                atm.start();
            }
            case "demo" -> {
                // Start demo mode
                DemoATM.main(null);
            }
            case "swing" -> {
                // Start Swing GUI mode using lambdas
                SwingUtilities.invokeLater(() -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    var app = new ATMSwingApp();
                    app.setVisible(true);
                });
            }
            default -> {
                System.out.println("Unknown mode: " + mode);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        // Using text blocks introduced in Java 15
        System.out.println("""
            Usage: java ModernATMSystem [mode]
            Available modes:
              console - Start the console-based ATM interface
              demo    - Run a demonstration of ATM features
              swing   - Start the Swing GUI interface (default)
            """);
    }
}

/**
 * TransactionDetails - Immutable record of transaction information
 *
 * I'm using Java 16's record feature here to reduce boilerplate.
 * This was so much easier than writing all the getters, equals, hashCode etc!
 *
 * Note to self: Need to eventually add transaction ID for audit purposes
 */
record TransactionDetails(
        String source,          // Source account number
        String destination,     // Destination account number
        double amount,          // Transaction amount in rupees
        String description,     // Transaction description
        LocalDateTime timestamp // When the transaction occurred
) {
    // Format the timestamp in a readable format
    public String formattedTimestamp() {
        // DD-MM-YYYY format is more common in India, but bank statements
        // typically use international format, so sticking with that
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Gets nicely formatted amount with ₹ symbol
    public String formattedAmount() {
        // Using Indian locale for Rupee symbol
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(amount);
    }
}

/**
 * Represents a transaction in the ATM system.
 */
class Transaction {
    private final TransactionType type;
    private final double amount;
    private final String description;
    private final Date timestamp;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;

    /**
     * Constructs a new Transaction.
     */
    public Transaction(
            TransactionType type,
            double amount,
            String description,
            Date timestamp,
            String sourceAccountNumber,
            String destinationAccountNumber) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
    }

    // Getters
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public Date getTimestamp() { return timestamp; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }

    // Convert to TransactionDetails record
    public TransactionDetails toDetails() {
        return new TransactionDetails(
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                description,
                LocalDateTime.now() // In a real app, we would convert the timestamp to LocalDateTime
        );
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("%s | %s | ₹%.2f | %s | From: %s | To: %s",
                dateFormat.format(timestamp),
                type,
                amount,
                description,
                sourceAccountNumber,
                destinationAccountNumber);
    }
}

/**
 * Enum for different types of transactions with additional utility methods.
 */
enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER("Transfer");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    // New utility method to check if this type involves cash
    public boolean involvesCash() {
        return this == DEPOSIT || this == WITHDRAWAL;
    }

    // Method to get the fee percentage, just as an example
    public double feePercentage() {
        return switch(this) {
            case DEPOSIT -> 0.0;
            case WITHDRAWAL -> 0.01; // 1% fee
            case TRANSFER -> 0.02;   // 2% fee
        };
    }
}

/**
 * Custom exception for insufficient funds.
 */
class InsufficientFundsException extends Exception {
    private final double requested;
    private final double available;

    public InsufficientFundsException(String message, double requested, double available) {
        super(message);
        this.requested = requested;
        this.available = available;
    }

    public double getRequested() { return requested; }
    public double getAvailable() { return available; }

    @Override
    public String getMessage() {
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return super.getMessage() +
                " Requested: " + formatter.format(requested) +
                ", Available: " + formatter.format(available);
    }
}

/**
 * Represents a bank account in the ATM system.
 */
class Account {
    private final String accountNumber;
    private String pin;
    private final String accountHolder;
    private double balance;
    private final java.util.List<Transaction> transactionHistory;

    /**
     * Constructs a new Account with the given details.
     */
    public Account(String accountNumber, String pin, String accountHolder, double initialBalance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.accountHolder = accountHolder;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();

        // Record the initial deposit as a transaction
        if (initialBalance > 0) {
            this.transactionHistory.add(new Transaction(
                    TransactionType.DEPOSIT,
                    initialBalance,
                    "Initial deposit",
                    new Date(),
                    this.accountNumber,
                    this.accountNumber
            ));
        }
    }

    /**
     * Authenticates the user using a PIN.
     */
    public boolean authenticate(String inputPin) {
        return this.pin.equals(inputPin);
    }

    // Getters
    public double getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }

    /**
     * Gets the transaction history for this account.
     */
    public java.util.List<Transaction> getTransactionHistory() {
        return new ArrayList<>(transactionHistory); // Return a copy to maintain encapsulation
    }

    /**
     * Gets the transaction history filtered by type.
     * Using Java 8+ Stream API for filtering
     */
    public java.util.List<Transaction> getTransactionHistoryByType(TransactionType type) {
        return transactionHistory.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Gets the total transaction amount for a specific type.
     * Using Java 8+ Stream API for summing
     */
    public double getTotalForTransactionType(TransactionType type) {
        return transactionHistory.stream()
                .filter(t -> t.getType() == type)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Deposits money into the account.
     */
    public boolean deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        balance += amount;

        // Record the transaction
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                amount,
                "Deposit to account",
                new Date(),
                this.accountNumber,
                this.accountNumber
        );
        transactionHistory.add(transaction);

        return true;
    }

    /**
     * Withdraws money from the account.
     */
    public boolean withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Insufficient funds for withdrawal",
                    amount,
                    balance
            );
        }

        balance -= amount;

        // Record the transaction
        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL,
                amount,
                "Withdrawal from account",
                new Date(),
                this.accountNumber,
                this.accountNumber
        );
        transactionHistory.add(transaction);

        return true;
    }

    /**
     * Transfers money to another account.
     */
    public boolean transfer(Account destinationAccount, double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Insufficient funds for transfer",
                    amount,
                    balance
            );
        }

        // Update balances
        balance -= amount;
        destinationAccount.balance += amount;

        // Record the transaction in the source account
        Transaction sourceTransaction = new Transaction(
                TransactionType.TRANSFER,
                amount,
                "Transfer to account " + destinationAccount.getAccountNumber(),
                new Date(),
                this.accountNumber,
                destinationAccount.getAccountNumber()
        );
        transactionHistory.add(sourceTransaction);

        // Record the transaction in the destination account
        Transaction destinationTransaction = new Transaction(
                TransactionType.TRANSFER,
                amount,
                "Transfer from account " + this.accountNumber,
                new Date(),
                this.accountNumber,
                destinationAccount.getAccountNumber()
        );
        destinationAccount.transactionHistory.add(destinationTransaction);

        return true;
    }

    /**
     * Changes the account PIN.
     */
    public boolean changePin(String oldPin, String newPin) {
        if (!authenticate(oldPin)) {
            return false;
        }

        this.pin = newPin;
        return true;
    }

    @Override
    public String toString() {
        return "Account [accountNumber=" + accountNumber + ", accountHolder=" + accountHolder
                + ", balance=" + balance + "]";
    }
}

/**
 * Represents a bank that manages accounts in the ATM system.
 */
class Bank {
    private final Map<String, Account> accounts;
    private final String name;

    /**
     * Constructs a new Bank with the given name.
     */
    public Bank(String name) {
        this.name = name;
        this.accounts = new HashMap<>();

        // For demonstration purposes, initialize the bank with some test accounts
        initializeTestAccounts();
    }

    /**
     * Gets the name of the bank.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the account with the specified account number.
     */
    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    /**
     * Adds an account to the bank.
     */
    public boolean addAccount(Account account) {
        if (accounts.containsKey(account.getAccountNumber())) {
            return false;
        }

        accounts.put(account.getAccountNumber(), account);
        return true;
    }

    /**
     * Authenticates a user by account number and PIN.
     */
    public Account authenticateUser(String accountNumber, String pin) {
        Account account = getAccount(accountNumber);

        if (account != null && account.authenticate(pin)) {
            return account;
        }

        return null;
    }

    /**
     * Gets all accounts in the bank.
     * Using Java 8+ approach for conversion
     */
    public java.util.List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    /**
     * Search for accounts by a predicate
     * Using Java 8+ Predicate for flexible filtering
     */
    public java.util.List<Account> findAccounts(Predicate<Account> criteria) {
        return accounts.values().stream()
                .filter(criteria)
                .collect(Collectors.toList());
    }

    /**
     * Search for accounts containing the given text in the holder's name
     * Using Java 8+ features for searching
     */
    public java.util.List<Account> searchByHolderName(String searchText) {
        return findAccounts(account ->
                account.getAccountHolder().toLowerCase().contains(searchText.toLowerCase()));
    }

    /**
     * Find accounts with balance greater than the given amount
     * Using Java 8+ features for filtering
     */
    public java.util.List<Account> findAccountsWithBalanceAbove(double minBalance) {
        return findAccounts(account -> account.getBalance() > minBalance);
    }

    /**
     * Initializes the bank with some test accounts.
     */
    private void initializeTestAccounts() {
        // Initialize with some typical Indian names and reasonable balances
        addAccount(new Account("123456", "1234", "Rajesh Kumar", 50000.0));
        addAccount(new Account("234567", "2345", "Priya Sharma", 35000.0));
        addAccount(new Account("345678", "3456", "Amit Patel", 72000.0));
        addAccount(new Account("456789", "4567", "Sunita Verma", 28000.0));
    }
}

/**
 * Represents an ATM that provides banking services through a console interface.
 */
class ATM {
    private final Bank bank;
    private Account currentAccount;
    private final BufferedReader reader;
    private boolean isRunning;

    /**
     * Constructs a new ATM.
     */
    public ATM() {
        this.bank = new Bank("State Bank of India");
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.isRunning = false;
    }

    /**
     * Starts the ATM.
     */
    public void start() {
        isRunning = true;
        displayWelcomeScreen();

        while (isRunning) {
            if (currentAccount == null) {
                authenticateUser();
            } else {
                showMainMenu();
            }
        }

        System.out.println("Thank you for using " + bank.getName() + " ATM. Goodbye!");
        try {
            reader.close();
        } catch (Exception e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Displays the welcome screen.
     */
    private void displayWelcomeScreen() {
        System.out.println("""
            ================================================
                  Welcome to %s ATM
            ================================================
            """.formatted(bank.getName()));
    }

    /**
     * Authenticates a user by prompting for account number and PIN.
     */
    private void authenticateUser() {
        System.out.println("\nPlease login to your account:");

        try {
            System.out.print("Enter your account number: ");
            String accountNumber = reader.readLine();

            System.out.print("Enter your PIN: ");
            String pin = reader.readLine();

            Account account = bank.authenticateUser(accountNumber, pin);

            if (account != null) {
                currentAccount = account;
                System.out.println("\nAuthentication successful!");
                System.out.println("Welcome, " + currentAccount.getAccountHolder() + "!");
            } else {
                System.out.println("\nAuthentication failed. Invalid account number or PIN.");
                System.out.println("Please try again.");
            }
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }

    /**
     * Shows the main menu and processes user selections.
     */
    private void showMainMenu() {
        // Using text blocks for cleaner menu representation
        System.out.println("""
            
            ================================================
                               MAIN MENU
            ================================================
            1. Check Balance
            2. Deposit Funds
            3. Withdraw Funds
            4. Transfer Funds
            5. View Transaction History
            6. Change PIN
            7. Logout
            8. Exit
            ================================================
            Enter your choice (1-8): """);

        try {
            int choice = Integer.parseInt(reader.readLine());

            // Using switch expressions
            switch (choice) {
                case 1 -> checkBalance();
                case 2 -> depositFunds();
                case 3 -> withdrawFunds();
                case 4 -> transferFunds();
                case 5 -> viewTransactionHistory();
                case 6 -> changePin();
                case 7 -> logout();
                case 8 -> isRunning = false;
                default -> System.out.println("Invalid choice. Please enter a number between 1 and 8.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }

    /**
     * Checks and displays the current account balance.
     */
    private void checkBalance() {
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        System.out.println("""
            
            ================================================
                           ACCOUNT BALANCE
            ================================================
            Account Number: %s
            Account Holder: %s
            Current Balance: %s
            ================================================
            """.formatted(
                currentAccount.getAccountNumber(),
                currentAccount.getAccountHolder(),
                formatter.format(currentAccount.getBalance())
        ));

        pressEnterToContinue();
    }

    /**
     * Processes a deposit transaction.
     */
    private void depositFunds() {
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        System.out.println("""
            
            ================================================
                            DEPOSIT FUNDS
            ================================================
            Current Balance: %s
            """.formatted(formatter.format(currentAccount.getBalance())));

        try {
            System.out.print("Enter amount to deposit: ₹");
            double amount = Double.parseDouble(reader.readLine());

            if (amount <= 0) {
                System.out.println("Invalid amount. Please enter a positive number.");
                return;
            }

            boolean success = currentAccount.deposit(amount);

            if (success) {
                System.out.println("\nDeposit successful!");
                System.out.println("New Balance: " + formatter.format(currentAccount.getBalance()));
            } else {
                System.out.println("\nDeposit failed. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid amount.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    /**
     * Processes a withdrawal transaction.
     */
    private void withdrawFunds() {
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        System.out.println("""
            
            ================================================
                           WITHDRAW FUNDS
            ================================================
            Current Balance: %s
            """.formatted(formatter.format(currentAccount.getBalance())));

        try {
            System.out.print("Enter amount to withdraw: ₹");
            double amount = Double.parseDouble(reader.readLine());

            if (amount <= 0) {
                System.out.println("Invalid amount. Please enter a positive number.");
                return;
            }

            boolean success = currentAccount.withdraw(amount);

            if (success) {
                System.out.println("\nWithdrawal successful!");
                System.out.println("New Balance: " + formatter.format(currentAccount.getBalance()));
            } else {
                System.out.println("\nWithdrawal failed. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid amount.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    /**
     * Processes a fund transfer transaction.
     */
    private void transferFunds() {
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        System.out.println("""
            
            ================================================
                           TRANSFER FUNDS
            ================================================
            Current Balance: %s
            """.formatted(formatter.format(currentAccount.getBalance())));

        try {
            System.out.print("Enter destination account number: ");
            String destinationAccountNumber = reader.readLine();

            if (destinationAccountNumber.equals(currentAccount.getAccountNumber())) {
                System.out.println("Cannot transfer to same account.");
                pressEnterToContinue();
                return;
            }

            Account destinationAccount = bank.getAccount(destinationAccountNumber);

            if (destinationAccount == null) {
                System.out.println("Destination account not found.");
                pressEnterToContinue();
                return;
            }

            System.out.print("Enter amount to transfer: ₹");
            double amount = Double.parseDouble(reader.readLine());

            if (amount <= 0) {
                System.out.println("Invalid amount. Please enter a positive number.");
                pressEnterToContinue();
                return;
            }

            System.out.println("""
                
                Transfer Details:
                From: %s (%s)
                To: %s (%s)
                Amount: %s
                """.formatted(
                    currentAccount.getAccountNumber(),
                    currentAccount.getAccountHolder(),
                    destinationAccount.getAccountNumber(),
                    destinationAccount.getAccountHolder(),
                    formatter.format(amount)
            ));

            System.out.print("Confirm transfer? (yes/no): ");
            String confirm = reader.readLine();

            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("Transfer cancelled.");
                pressEnterToContinue();
                return;
            }

            boolean success = currentAccount.transfer(destinationAccount, amount);

            if (success) {
                System.out.println("\nTransfer successful!");
                System.out.println("New Balance: " + formatter.format(currentAccount.getBalance()));
            } else {
                System.out.println("\nTransfer failed. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid amount.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    /**
     * Displays the transaction history for the current account.
     */
    private void viewTransactionHistory() {
        System.out.println("""
            
            ================================================
                        TRANSACTION HISTORY
            ================================================
            """);

        var transactions = currentAccount.getTransactionHistory();

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("Date & Time            | Type       | Amount    | Description");
            System.out.println("---------------------------------------------------------------------");

            // Using modern for-each loop with formatted output
            transactions.forEach(transaction -> {
                String formattedDate = dateFormat.format(transaction.getTimestamp());
                String type = transaction.getType().toString();
                String amount = String.format("₹%.2f", transaction.getAmount());

                System.out.printf("%-22s | %-10s | %-9s | %s%n",
                        formattedDate, type, amount, transaction.getDescription());
            });

            // Also show transaction statistics
            System.out.println("\nTransaction Statistics:");
            System.out.println("---------------------------------------------------------------------");

            var depositTotal = currentAccount.getTotalForTransactionType(TransactionType.DEPOSIT);
            var withdrawalTotal = currentAccount.getTotalForTransactionType(TransactionType.WITHDRAWAL);
            var transferTotal = currentAccount.getTotalForTransactionType(TransactionType.TRANSFER);

            System.out.printf("Total Deposits:    ₹%.2f%n", depositTotal);
            System.out.printf("Total Withdrawals: ₹%.2f%n", withdrawalTotal);
            System.out.printf("Total Transfers:   ₹%.2f%n", transferTotal);
        }

        pressEnterToContinue();
    }

    /**
     * Changes the PIN for the current account.
     */
    private void changePin() {
        System.out.println("""
            
            ================================================
                            CHANGE PIN
            ================================================
            """);

        try {
            System.out.print("Enter your current PIN: ");
            String currentPin = reader.readLine();

            System.out.print("Enter your new PIN: ");
            String newPin = reader.readLine();

            System.out.print("Confirm your new PIN: ");
            String confirmPin = reader.readLine();

            if (!newPin.equals(confirmPin)) {
                System.out.println("PINs do not match. PIN change cancelled.");
                pressEnterToContinue();
                return;
            }

            boolean success = currentAccount.changePin(currentPin, newPin);

            if (success) {
                System.out.println("PIN changed successfully!");
            } else {
                System.out.println("PIN change failed. Incorrect current PIN.");
            }
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    /**
     * Logs out the current user.
     */
    private void logout() {
        System.out.println("\nLogging out...");
        currentAccount = null;
    }

    /**
     * Prompts the user to press Enter to continue.
     */
    private void pressEnterToContinue() {
        System.out.println("\nPress Enter to continue...");
        try {
            reader.readLine();
        } catch (Exception e) {
            System.out.println("Error reading input: " + e.getMessage());
        }
    }
}

/**
 * A demonstration class that shows how to use the ATM system with sample inputs.
 * This class provides an alternative to the interactive ATM that doesn't require user input.
 *
 * Created this for quick demos and testing during development.
 * It's a simple way to showcase features without having to go through
 * the full GUI or console interface each time.
 *
 * @author Rahul Verma
 * @version 1.3
 * @since 15-Apr-2025
 */
class DemoATM {
    public static void main(String[] args) {
        System.out.println("""
            SBI ATM Simulation System - Demo Mode
            ===================================
            """);

        // Create bank and account objects
        var bank = new Bank("State Bank of India");

        // Get one of the test accounts
        var account = bank.getAccount("123456");

        if (account != null) {
            // Use Indian Rupee format
            var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            System.out.println("""
                
                Account Information:
                Account Number: %s
                Account Holder: %s
                Initial Balance: %s
                
                Performing transactions...
                """.formatted(
                    account.getAccountNumber(),
                    account.getAccountHolder(),
                    formatter.format(account.getBalance())
            ));

            try {
                // Perform a deposit
                double depositAmount = 500.0;
                account.deposit(depositAmount);
                System.out.println("Deposited: " + formatter.format(depositAmount));
                System.out.println("New Balance: " + formatter.format(account.getBalance()));

                // Perform a withdrawal
                double withdrawalAmount = 200.0;
                account.withdraw(withdrawalAmount);
                System.out.println("\nWithdrawn: " + formatter.format(withdrawalAmount));
                System.out.println("New Balance: " + formatter.format(account.getBalance()));

                // Perform a transfer
                Account destinationAccount = bank.getAccount("234567");
                double transferAmount = 300.0;

                if (destinationAccount != null) {
                    double initialDestBalance = destinationAccount.getBalance();

                    account.transfer(destinationAccount, transferAmount);

                    System.out.println("""
                        
                        Transferred: %s
                        From Account: %s (%s)
                        To Account: %s (%s)
                        New Source Balance: %s
                        New Destination Balance: %s
                        """.formatted(
                            formatter.format(transferAmount),
                            account.getAccountNumber(),
                            account.getAccountHolder(),
                            destinationAccount.getAccountNumber(),
                            destinationAccount.getAccountHolder(),
                            formatter.format(account.getBalance()),
                            formatter.format(destinationAccount.getBalance())
                    ));
                }

                // Show transaction history
                System.out.println("""
                    
                    Transaction History:
                    ====================
                    """);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // Using stream and lambdas to print transaction history
                account.getTransactionHistory()
                        .forEach(transaction ->
                                System.out.println(dateFormat.format(transaction.getTimestamp()) + " | " +
                                        transaction.getType() + " | " +
                                        formatter.format(transaction.getAmount()) + " | " +
                                        transaction.getDescription())
                        );

                // Show transaction statistics using the new methods
                System.out.println("""
                    
                    Transaction Statistics:
                    ====================
                    """);

                System.out.println("Total deposits: " +
                        formatter.format(account.getTotalForTransactionType(TransactionType.DEPOSIT)));
                System.out.println("Total withdrawals: " +
                        formatter.format(account.getTotalForTransactionType(TransactionType.WITHDRAWAL)));
                System.out.println("Total transfers: " +
                        formatter.format(account.getTotalForTransactionType(TransactionType.TRANSFER)));

            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        } else {
            System.out.println("Test account not found!");
        }

        System.out.println("""
            
            Demo completed. Run 'java ModernATMSystem' for the full interactive SBI ATM experience.
            """);
    }
}

/**
 * A Swing-based GUI for the ATM Simulation System
 *
 * This was challenging to implement on Replit since JavaFX wasn't
 * working well, but Swing still does the job nicely. I've tried to
 * make it look as modern as possible despite Swing's limitations.
 *
 * @author Rahul Verma
 * @version 2.1
 */
class ATMSwingApp extends JFrame {
    // Model components
    private final Bank bank;
    private Account currentAccount;

    // UI Components
    private JPanel loginPanel;
    private JPanel mainMenuPanel;
    private JPanel balancePanel;
    private JPanel depositPanel;
    private JPanel withdrawPanel;
    private JPanel transferPanel;
    private JPanel historyPanel;
    private JPanel changePinPanel;

    // Login components
    private JTextField accountField;
    private JPasswordField pinField;
    private JLabel statusLabel;

    // Counter for login attempts (using Atomic classes for thread safety)
    private final AtomicInteger loginAttempts = new AtomicInteger(0);
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    /**
     * Constructor initializes the ATM application
     */
    public ATMSwingApp() {
        this.bank = new Bank("State Bank of India");

        // Set up the JFrame
        setTitle("SBI ATM Simulation System");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize panels
        createLoginPanel();

        // Display the login panel
        getContentPane().add(loginPanel);
    }

    /**
     * Creates the login panel
     */
    private void createLoginPanel() {
        loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Welcome to " + bank.getName());
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Account field
        JPanel accountPanel = new JPanel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel accountLabel = new JLabel("Account Number: ");
        accountField = new JTextField(10);
        accountPanel.add(accountLabel);
        accountPanel.add(accountField);

        // PIN field
        JPanel pinPanel = new JPanel();
        pinPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel pinLabel = new JLabel("PIN: ");
        pinField = new JPasswordField(10);
        pinPanel.add(pinLabel);
        pinPanel.add(pinField);

        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label for error messages
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Test accounts info
        JTextArea testAccountsText = new JTextArea("""
            Test Accounts:
            Account: 123456, PIN: 1234
            Account: 234567, PIN: 2345
            Account: 345678, PIN: 3456
            Account: 456789, PIN: 4567
            """);
        testAccountsText.setEditable(false);
        testAccountsText.setBackground(null);
        testAccountsText.setFont(new Font("Arial", Font.PLAIN, 12));
        testAccountsText.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(headerLabel);
        loginPanel.add(Box.createVerticalStrut(40));
        loginPanel.add(accountPanel);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(pinPanel);
        loginPanel.add(Box.createVerticalStrut(20));
        loginPanel.add(loginButton);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(statusLabel);
        loginPanel.add(Box.createVerticalStrut(40));
        loginPanel.add(testAccountsText);

        // Login button action
        loginButton.addActionListener(e -> {
            String accountNumber = accountField.getText();
            String pin = new String(pinField.getPassword());

            // Check if max attempts reached
            if (loginAttempts.get() >= MAX_LOGIN_ATTEMPTS) {
                statusLabel.setText("Too many failed attempts. Please try again later.");
                loginButton.setEnabled(false);

                // Re-enable after a delay (in a real app, we might use more sophisticated logic)
                Timer timer = new Timer(10000, event -> {
                    loginButton.setEnabled(true);
                    loginAttempts.set(0);
                    statusLabel.setText("You can try again now.");
                });
                timer.setRepeats(false);
                timer.start();
                return;
            }

            currentAccount = bank.authenticateUser(accountNumber, pin);

            if (currentAccount != null) {
                createMainMenuPanel();
                getContentPane().removeAll();
                getContentPane().add(mainMenuPanel);
                revalidate();
                repaint();
                statusLabel.setText(" ");
                // Reset login attempts on successful login
                loginAttempts.set(0);
            } else {
                loginAttempts.incrementAndGet();
                int remainingAttempts = MAX_LOGIN_ATTEMPTS - loginAttempts.get();
                statusLabel.setText("Invalid account number or PIN. " +
                        (remainingAttempts > 0 ? remainingAttempts + " attempts remaining." : ""));
            }
        });
    }

    /**
     * Creates the main menu panel
     */
    private void createMainMenuPanel() {
        mainMenuPanel = new JPanel();
        mainMenuPanel.setLayout(new BoxLayout(mainMenuPanel, BoxLayout.Y_AXIS));
        mainMenuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Main Menu");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome, " + currentAccount.getAccountHolder() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Account balance overview
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        JLabel balanceLabel = new JLabel("Current Balance: " +
                formatter.format(currentAccount.getBalance()));
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        Dimension buttonSize = new Dimension(200, 30);

        JButton checkBalanceButton = new JButton("Check Balance");
        checkBalanceButton.setMaximumSize(buttonSize);
        checkBalanceButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton depositButton = new JButton("Deposit Funds");
        depositButton.setMaximumSize(buttonSize);
        depositButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton withdrawButton = new JButton("Withdraw Funds");
        withdrawButton.setMaximumSize(buttonSize);
        withdrawButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton transferButton = new JButton("Transfer Funds");
        transferButton.setMaximumSize(buttonSize);
        transferButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton historyButton = new JButton("Transaction History");
        historyButton.setMaximumSize(buttonSize);
        historyButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton changePinButton = new JButton("Change PIN");
        changePinButton.setMaximumSize(buttonSize);
        changePinButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setMaximumSize(buttonSize);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        mainMenuPanel.add(Box.createVerticalStrut(20));
        mainMenuPanel.add(headerLabel);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(welcomeLabel);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(balanceLabel);
        mainMenuPanel.add(Box.createVerticalStrut(30));
        mainMenuPanel.add(checkBalanceButton);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(depositButton);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(withdrawButton);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(transferButton);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(historyButton);
        mainMenuPanel.add(Box.createVerticalStrut(10));
        mainMenuPanel.add(changePinButton);
        mainMenuPanel.add(Box.createVerticalStrut(30));
        mainMenuPanel.add(logoutButton);

        // Button actions
        checkBalanceButton.addActionListener(e -> {
            createBalancePanel();
            showPanel(balancePanel);
        });

        depositButton.addActionListener(e -> {
            createDepositPanel();
            showPanel(depositPanel);
        });

        withdrawButton.addActionListener(e -> {
            createWithdrawPanel();
            showPanel(withdrawPanel);
        });

        transferButton.addActionListener(e -> {
            createTransferPanel();
            showPanel(transferPanel);
        });

        historyButton.addActionListener(e -> {
            createHistoryPanel();
            showPanel(historyPanel);
        });

        changePinButton.addActionListener(e -> {
            createChangePinPanel();
            showPanel(changePinPanel);
        });

        logoutButton.addActionListener(e -> {
            currentAccount = null;
            accountField.setText("");
            pinField.setText("");
            showPanel(loginPanel);
        });
    }

    /**
     * Creates the balance panel
     */
    private void createBalancePanel() {
        balancePanel = new JPanel();
        balancePanel.setLayout(new BoxLayout(balancePanel, BoxLayout.Y_AXIS));
        balancePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Formatter for currency
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        JLabel headerLabel = new JLabel("Account Balance");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Account info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 1, 0, 10));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel accountNumberLabel = new JLabel("Account Number: " + currentAccount.getAccountNumber());
        JLabel accountHolderLabel = new JLabel("Account Holder: " + currentAccount.getAccountHolder());
        JLabel balanceLabel = new JLabel("Current Balance: " +
                formatter.format(currentAccount.getBalance()));
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));

        infoPanel.add(accountNumberLabel);
        infoPanel.add(accountHolderLabel);
        infoPanel.add(balanceLabel);

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        balancePanel.add(headerLabel);
        balancePanel.add(Box.createVerticalStrut(20));
        balancePanel.add(infoPanel);
        balancePanel.add(Box.createVerticalStrut(40));
        balancePanel.add(backButton);

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Creates the deposit panel
     */
    private void createDepositPanel() {
        depositPanel = new JPanel();
        depositPanel.setLayout(new BoxLayout(depositPanel, BoxLayout.Y_AXIS));
        depositPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Formatter for currency
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        JLabel headerLabel = new JLabel("Deposit Funds");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Current balance
        JLabel balanceLabel = new JLabel("Current Balance: " +
                formatter.format(currentAccount.getBalance()));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Amount input
        JPanel amountPanel = new JPanel();
        amountPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel amountLabel = new JLabel("Enter amount to deposit: ₹");
        JTextField amountField = new JTextField(10);
        amountPanel.add(amountLabel);
        amountPanel.add(amountField);

        // Deposit button
        JButton depositButton = new JButton("Deposit");
        depositButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        depositPanel.add(headerLabel);
        depositPanel.add(Box.createVerticalStrut(20));
        depositPanel.add(balanceLabel);
        depositPanel.add(Box.createVerticalStrut(30));
        depositPanel.add(amountPanel);
        depositPanel.add(Box.createVerticalStrut(20));
        depositPanel.add(depositButton);
        depositPanel.add(Box.createVerticalStrut(10));
        depositPanel.add(statusLabel);
        depositPanel.add(Box.createVerticalStrut(30));
        depositPanel.add(backButton);

        // Deposit button action
        depositButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());

                if (amount <= 0) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Invalid amount. Please enter a positive number.");
                    return;
                }

                boolean success = currentAccount.deposit(amount);

                if (success) {
                    statusLabel.setForeground(Color.GREEN.darker());
                    statusLabel.setText("Deposit successful! New balance: " +
                            formatter.format(currentAccount.getBalance()));
                    balanceLabel.setText("Current Balance: " +
                            formatter.format(currentAccount.getBalance()));
                    amountField.setText("");
                } else {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Deposit failed. Please try again.");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Invalid input. Please enter a valid amount.");
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Creates the withdraw panel
     */
    private void createWithdrawPanel() {
        withdrawPanel = new JPanel();
        withdrawPanel.setLayout(new BoxLayout(withdrawPanel, BoxLayout.Y_AXIS));
        withdrawPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Formatter for currency
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        JLabel headerLabel = new JLabel("Withdraw Funds");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Current balance
        JLabel balanceLabel = new JLabel("Current Balance: " +
                formatter.format(currentAccount.getBalance()));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Amount input
        JPanel amountPanel = new JPanel();
        amountPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel amountLabel = new JLabel("Enter amount to withdraw: ₹");
        JTextField amountField = new JTextField(10);
        amountPanel.add(amountLabel);
        amountPanel.add(amountField);

        // Quick withdrawal buttons
        JPanel quickWithdrawPanel = new JPanel();
        quickWithdrawPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton btn20 = new JButton("₹20");
        JButton btn50 = new JButton("₹50");
        JButton btn100 = new JButton("₹100");
        JButton btn200 = new JButton("₹200");

        quickWithdrawPanel.add(btn20);
        quickWithdrawPanel.add(btn50);
        quickWithdrawPanel.add(btn100);
        quickWithdrawPanel.add(btn200);

        // Withdraw button
        JButton withdrawButton = new JButton("Withdraw");
        withdrawButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        withdrawPanel.add(headerLabel);
        withdrawPanel.add(Box.createVerticalStrut(20));
        withdrawPanel.add(balanceLabel);
        withdrawPanel.add(Box.createVerticalStrut(20));
        withdrawPanel.add(amountPanel);
        withdrawPanel.add(Box.createVerticalStrut(10));
        withdrawPanel.add(new JLabel("Quick Withdraw:"));
        withdrawPanel.add(quickWithdrawPanel);
        withdrawPanel.add(Box.createVerticalStrut(20));
        withdrawPanel.add(withdrawButton);
        withdrawPanel.add(Box.createVerticalStrut(10));
        withdrawPanel.add(statusLabel);
        withdrawPanel.add(Box.createVerticalStrut(30));
        withdrawPanel.add(backButton);

        // Quick withdraw button actions
        btn20.addActionListener(e -> amountField.setText("20"));
        btn50.addActionListener(e -> amountField.setText("50"));
        btn100.addActionListener(e -> amountField.setText("100"));
        btn200.addActionListener(e -> amountField.setText("200"));

        // Withdraw button action
        withdrawButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());

                if (amount <= 0) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Invalid amount. Please enter a positive number.");
                    return;
                }

                boolean success = currentAccount.withdraw(amount);

                if (success) {
                    statusLabel.setForeground(Color.GREEN.darker());
                    statusLabel.setText("Withdrawal successful! New balance: " +
                            formatter.format(currentAccount.getBalance()));
                    balanceLabel.setText("Current Balance: " +
                            formatter.format(currentAccount.getBalance()));
                    amountField.setText("");
                } else {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Withdrawal failed. Please try again.");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Invalid input. Please enter a valid amount.");
            } catch (InsufficientFundsException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Error: " + ex.getMessage());
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Creates the transfer panel
     */
    private void createTransferPanel() {
        transferPanel = new JPanel();
        transferPanel.setLayout(new BoxLayout(transferPanel, BoxLayout.Y_AXIS));
        transferPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Formatter for currency
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        JLabel headerLabel = new JLabel("Transfer Funds");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Current balance
        JLabel balanceLabel = new JLabel("Current Balance: " +
                formatter.format(currentAccount.getBalance()));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Destination account input
        JPanel destAccountPanel = new JPanel();
        destAccountPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel destAccountLabel = new JLabel("Destination Account: ");
        JTextField destAccountField = new JTextField(10);
        JButton verifyButton = new JButton("Verify");
        destAccountPanel.add(destAccountLabel);
        destAccountPanel.add(destAccountField);
        destAccountPanel.add(verifyButton);

        // Verification result
        JLabel verificationLabel = new JLabel(" ");
        verificationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Amount input
        JPanel amountPanel = new JPanel();
        amountPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel amountLabel = new JLabel("Amount to Transfer: ₹");
        JTextField amountField = new JTextField(10);
        amountPanel.add(amountLabel);
        amountPanel.add(amountField);

        // Transfer button
        JButton transferButton = new JButton("Transfer");
        transferButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        transferButton.setEnabled(false); // Disabled until account is verified

        // Status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        transferPanel.add(headerLabel);
        transferPanel.add(Box.createVerticalStrut(20));
        transferPanel.add(balanceLabel);
        transferPanel.add(Box.createVerticalStrut(20));
        transferPanel.add(destAccountPanel);
        transferPanel.add(Box.createVerticalStrut(5));
        transferPanel.add(verificationLabel);
        transferPanel.add(Box.createVerticalStrut(10));
        transferPanel.add(amountPanel);
        transferPanel.add(Box.createVerticalStrut(20));
        transferPanel.add(transferButton);
        transferPanel.add(Box.createVerticalStrut(10));
        transferPanel.add(statusLabel);
        transferPanel.add(Box.createVerticalStrut(30));
        transferPanel.add(backButton);

        // Verify button action
        verifyButton.addActionListener(e -> {
            String destinationAccountNumber = destAccountField.getText();

            if (destinationAccountNumber.equals(currentAccount.getAccountNumber())) {
                verificationLabel.setForeground(Color.RED);
                verificationLabel.setText("Cannot transfer to the same account");
                transferButton.setEnabled(false);
                return;
            }

            Account destinationAccount = bank.getAccount(destinationAccountNumber);

            if (destinationAccount != null) {
                verificationLabel.setForeground(Color.GREEN.darker());
                verificationLabel.setText("Account verified: " + destinationAccount.getAccountHolder());
                transferButton.setEnabled(true);
            } else {
                verificationLabel.setForeground(Color.RED);
                verificationLabel.setText("Account not found");
                transferButton.setEnabled(false);
            }
        });

        // Transfer button action
        transferButton.addActionListener(e -> {
            try {
                String destinationAccountNumber = destAccountField.getText();
                Account destinationAccount = bank.getAccount(destinationAccountNumber);

                if (destinationAccount == null) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Destination account not found");
                    return;
                }

                double amount = Double.parseDouble(amountField.getText());

                if (amount <= 0) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Invalid amount. Please enter a positive number.");
                    return;
                }

                boolean success = currentAccount.transfer(destinationAccount, amount);

                if (success) {
                    statusLabel.setForeground(Color.GREEN.darker());
                    statusLabel.setText("Transfer successful! New balance: " +
                            formatter.format(currentAccount.getBalance()));
                    balanceLabel.setText("Current Balance: " +
                            formatter.format(currentAccount.getBalance()));
                    amountField.setText("");
                    transferButton.setEnabled(false);
                    verificationLabel.setText(" ");
                    destAccountField.setText("");
                } else {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Transfer failed. Please try again.");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Invalid input. Please enter a valid amount.");
            } catch (InsufficientFundsException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Error: " + ex.getMessage());
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Creates the transaction history panel
     */
    private void createHistoryPanel() {
        historyPanel = new JPanel();
        historyPanel.setLayout(new BorderLayout(10, 10));
        historyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Transaction History", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Transaction table
        String[] columnNames = {"Date & Time", "Type", "Amount", "Description"};
        java.util.List<Transaction> transactions = currentAccount.getTransactionHistory();
        Object[][] data = new Object[transactions.size()][4];

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            data[i][0] = dateFormat.format(t.getTimestamp());
            data[i][1] = t.getType().toString();
            data[i][2] = String.format("₹%.2f", t.getAmount());
            data[i][3] = t.getDescription();
        }

        JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(450, 300));
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        // Add sorting capability
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);

        // Statistics panel
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(3, 1));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Transaction Statistics"));

        // Formatter for currency
        // Using Indian Rupee format
        var formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Calculate totals
        double depositTotal = currentAccount.getTotalForTransactionType(TransactionType.DEPOSIT);
        double withdrawalTotal = currentAccount.getTotalForTransactionType(TransactionType.WITHDRAWAL);
        double transferTotal = currentAccount.getTotalForTransactionType(TransactionType.TRANSFER);

        JLabel depositsLabel = new JLabel("Total Deposits: " + formatter.format(depositTotal));
        JLabel withdrawalsLabel = new JLabel("Total Withdrawals: " + formatter.format(withdrawalTotal));
        JLabel transfersLabel = new JLabel("Total Transfers: " + formatter.format(transferTotal));

        statsPanel.add(depositsLabel);
        statsPanel.add(withdrawalsLabel);
        statsPanel.add(transfersLabel);

        // No transactions message
        JPanel contentPanel = new JPanel(new BorderLayout());
        if (transactions.isEmpty()) {
            contentPanel.add(new JLabel("No transactions found."), BorderLayout.CENTER);
        } else {
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            contentPanel.add(statsPanel, BorderLayout.SOUTH);
        }

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(backButton);

        // Add components to the panel
        historyPanel.add(headerLabel, BorderLayout.NORTH);
        historyPanel.add(contentPanel, BorderLayout.CENTER);
        historyPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Creates the change PIN panel
     */
    private void createChangePinPanel() {
        changePinPanel = new JPanel();
        changePinPanel.setLayout(new BoxLayout(changePinPanel, BoxLayout.Y_AXIS));
        changePinPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Change PIN");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Current PIN input
        JPanel currentPinPanel = new JPanel();
        currentPinPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel currentPinLabel = new JLabel("Current PIN: ");
        JPasswordField currentPinField = new JPasswordField(10);
        currentPinPanel.add(currentPinLabel);
        currentPinPanel.add(currentPinField);

        // New PIN input
        JPanel newPinPanel = new JPanel();
        newPinPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel newPinLabel = new JLabel("New PIN: ");
        JPasswordField newPinField = new JPasswordField(10);
        newPinPanel.add(newPinLabel);
        newPinPanel.add(newPinField);

        // Confirm PIN input
        JPanel confirmPinPanel = new JPanel();
        confirmPinPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel confirmPinLabel = new JLabel("Confirm New PIN: ");
        JPasswordField confirmPinField = new JPasswordField(10);
        confirmPinPanel.add(confirmPinLabel);
        confirmPinPanel.add(confirmPinField);

        // PIN strength indicator
        JProgressBar strengthBar = new JProgressBar(0, 4);
        strengthBar.setValue(0);
        strengthBar.setStringPainted(true);
        strengthBar.setString("PIN Strength");
        strengthBar.setMaximumSize(new Dimension(200, 20));
        strengthBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // PIN requirements info
        JLabel requirementsLabel = new JLabel("PIN should be at least 4 digits");
        requirementsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        requirementsLabel.setForeground(Color.GRAY);

        // Add document listener to new PIN field to calculate strength
        newPinField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }

            private void updateStrength() {
                String pin = new String(newPinField.getPassword());

                // Calculate strength (simple example)
                int strength = 0;
                if (pin.length() >= 4) strength++;
                if (pin.length() >= 6) strength++;
                if (!pin.matches("\\d+")) strength++; // Contains non-digits
                if (pin.matches(".*[!@#$%^&*()].*")) strength++; // Contains special chars

                strengthBar.setValue(strength);

                // Update color based on strength
                if (strength < 2) {
                    strengthBar.setForeground(Color.RED);
                    strengthBar.setString("Weak");
                } else if (strength < 3) {
                    strengthBar.setForeground(Color.YELLOW);
                    strengthBar.setString("Medium");
                } else {
                    strengthBar.setForeground(Color.GREEN);
                    strengthBar.setString("Strong");
                }
            }
        });

        // Change PIN button
        JButton changePinButton = new JButton("Change PIN");
        changePinButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Back button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to the panel
        changePinPanel.add(headerLabel);
        changePinPanel.add(Box.createVerticalStrut(30));
        changePinPanel.add(currentPinPanel);
        changePinPanel.add(Box.createVerticalStrut(10));
        changePinPanel.add(newPinPanel);
        changePinPanel.add(Box.createVerticalStrut(5));
        changePinPanel.add(strengthBar);
        changePinPanel.add(Box.createVerticalStrut(5));
        changePinPanel.add(requirementsLabel);
        changePinPanel.add(Box.createVerticalStrut(10));
        changePinPanel.add(confirmPinPanel);
        changePinPanel.add(Box.createVerticalStrut(20));
        changePinPanel.add(changePinButton);
        changePinPanel.add(Box.createVerticalStrut(10));
        changePinPanel.add(statusLabel);
        changePinPanel.add(Box.createVerticalStrut(30));
        changePinPanel.add(backButton);

        // Change PIN button action
        changePinButton.addActionListener(e -> {
            String currentPin = new String(currentPinField.getPassword());
            String newPin = new String(newPinField.getPassword());
            String confirmPin = new String(confirmPinField.getPassword());

            if (newPin.length() < 4) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("PIN must be at least 4 characters");
                return;
            }

            if (!newPin.equals(confirmPin)) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("PINs do not match. Please try again.");
                return;
            }

            boolean success = currentAccount.changePin(currentPin, newPin);

            if (success) {
                statusLabel.setForeground(Color.GREEN.darker());
                statusLabel.setText("PIN changed successfully!");
                currentPinField.setText("");
                newPinField.setText("");
                confirmPinField.setText("");
                strengthBar.setValue(0);
                strengthBar.setString("PIN Strength");
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("PIN change failed. Incorrect current PIN.");
            }
        });

        // Back button action
        backButton.addActionListener(e -> showPanel(mainMenuPanel));
    }

    /**
     * Shows the specified panel and hides all others
     *
     * Note: Had to implement this helper method after struggling with CardLayout,
     * which was not working well for this use case. This approach is cleaner anyway.
     *
     * - Rahul (22-Mar-2025)
     */
    private void showPanel(JPanel panel) {
        getContentPane().removeAll();
        getContentPane().add(panel);
        revalidate();
        repaint();
    }
}