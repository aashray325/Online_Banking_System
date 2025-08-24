package onlinebankingsystem;

import javafx.collections.FXCollections;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.File;
import java.io.InputStream;


public class DatabaseService {
    private String URL ;
    private String USER ;
    private String PASSWORD ;

    public DatabaseService() {
        loadDatabaseProperties();
    }

    private void loadDatabaseProperties() {
        Properties props = new Properties();
        try {
            // First try to load from the project root directory
            File configFile = new File("config.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    System.out.println("Loaded config from file: " + configFile.getAbsolutePath());
                }
            } else {
                // Fallback to classpath resource (useful for tests or when running from JAR)
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                    if (is != null) {
                        props.load(is);
                        System.out.println("Loaded config from classpath");
                    } else {
                        System.err.println("Could not find config.properties in classpath");
                    }
                }
            }

            // Load the properties
            this.URL = props.getProperty("db.URL");
            this.USER = props.getProperty("db.USER");
            this.PASSWORD = props.getProperty("db.PASSWORD");

            // Log loaded properties (for debugging, remove in production)
            System.out.println("Database URL: " + this.URL);
            System.out.println("Database User: " + this.USER);
            // Don't log the password!

            // Check if properties were loaded
            if (this.URL == null || this.USER == null || this.PASSWORD == null) {
                System.err.println("One or more database properties not found in config file");
                // Fallback to hardcoded values for development only (remove in production)
                this.URL = "jdbc:mysql://localhost:3306/online_banking_system";
                this.USER = "root";
                this.PASSWORD = ""; // Don't include your real password here
            }
        } catch (IOException e) {
            System.err.println("Failed to load database properties: " + e.getMessage());
            e.printStackTrace();
            // Fallback to hardcoded values for development only
            this.URL = "jdbc:mysql://localhost:3306/online_banking_system";
            this.USER = "root";
            this.PASSWORD = ""; // Don't include your real password here
        }
    }


    // Get connection to database
    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            throw e;
        }
    }

    // Customer methods
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {

            while (rs.next()) {
                customers.add(new Customer(
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getInt("phone"),
                        rs.getString("uid"),
                        rs.getString("password")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting customers: " + e.getMessage());
        }

        return customers;
    }

    public Customer getCustomerById(Long id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM customers WHERE id = ?")) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getInt("phone"),
                            rs.getString("uid"),
                            rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting customer: " + e.getMessage());
        }

        return null;
    }

    public boolean registerCustomer(String firstName, String lastName, int phone, String password) {
        // Call the new method with an initial balance of 0
        return registerCustomerWithInitialBalance(firstName, lastName, phone, password, 0);
    }

    // Account methods
    public List<Account> getAccountsByCustomerId(Long customerId) {
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM account WHERE customer_id = ?")) {

            pstmt.setLong(1, customerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(new Account(
                            rs.getLong("account_id"),
                            rs.getLong("customer_id"),
                            rs.getLong("balance"),
                            rs.getString("status"),
                            rs.getString("type")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting accounts: " + e.getMessage());
        }

        return accounts;
    }

    public boolean registerCustomerWithInitialBalance(String firstName, String lastName, int phone, String password, long initialBalance) {
        Connection conn = null;
        PreparedStatement authStmt = null;
        PreparedStatement customerStmt = null;
        PreparedStatement accountStmt = null;
        PreparedStatement transactionStmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction


            String uid = java.util.UUID.randomUUID().toString();


            String authSql = "INSERT INTO auth_users (id) VALUES (?)";
            authStmt = conn.prepareStatement(authSql);
            authStmt.setString(1, uid);
            authStmt.executeUpdate();

            String customerSql = "INSERT INTO customers (first_name, last_name, phone, uid, password) VALUES (?, ?, ?, ?, ?)";
            customerStmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS);
            customerStmt.setString(1, firstName);
            customerStmt.setString(2, lastName);
            customerStmt.setInt(3, phone);
            customerStmt.setString(4, uid);
            customerStmt.setString(5, password); // In a real app, hash this password

            int affectedRows = customerStmt.executeUpdate();
            long customerId = -1;

            if (affectedRows > 0) {
                generatedKeys = customerStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    customerId = generatedKeys.getLong(1);
                    System.out.println("New customer registered with ID: " + customerId);
                }
            } else {
                conn.rollback();
                return false;
            }


            if (customerId != -1) {
                String accountSql = "INSERT INTO account (customer_id, balance, status, type) VALUES (?, ?, ?, ?)";
                accountStmt = conn.prepareStatement(accountSql, Statement.RETURN_GENERATED_KEYS);
                accountStmt.setLong(1, customerId);
                accountStmt.setLong(2, initialBalance);  // Set the initial balance here
                accountStmt.setString(3, "active");
                accountStmt.setString(4, "savings");

                int accountRows = accountStmt.executeUpdate();
                long accountId = -1;

                if (accountRows > 0) {
                    generatedKeys.close();  // Close previous ResultSet
                    generatedKeys = accountStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        accountId = generatedKeys.getLong(1);
                        System.out.println("New account created with ID: " + accountId + " and balance: " + initialBalance);
                    }


                    if (initialBalance > 0 && accountId != -1) {
                        String transactionSql = "INSERT INTO transactions (type, toID, amount) VALUES (?, ?, ?)";
                        transactionStmt = conn.prepareStatement(transactionSql);
                        transactionStmt.setString(1, "initial_deposit");
                        transactionStmt.setLong(2, accountId);  // Use the account ID for the toID field
                        transactionStmt.setLong(3, initialBalance);

                        transactionStmt.executeUpdate();
                        System.out.println("Initial deposit transaction recorded: " + initialBalance);
                    }
                } else {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            // If something goes wrong, roll back the transaction
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            System.err.println("Error registering customer with initial balance: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (transactionStmt != null) transactionStmt.close();
                if (accountStmt != null) accountStmt.close();
                if (customerStmt != null) customerStmt.close();
                if (authStmt != null) authStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto commit
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }


    // Transaction methods
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM transactions WHERE fromID = ? OR toID = ? ORDER BY trans_id DESC")) {

            pstmt.setLong(1, accountId);
            pstmt.setLong(2, accountId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                            rs.getLong("trans_id"),
                            rs.getString("type"),
                            rs.getLong("fromID"),
                            rs.getLong("toID"),
                            rs.getLong("amount")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
        }

        return transactions;
    }

    // Transfer funds using stored procedure
    public boolean transferFunds(Long fromAccountId, Long toAccountId, Long amount) {
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL TransferFunds(?, ?, ?)}")) {

            cstmt.setLong(1, fromAccountId);
            cstmt.setLong(2, toAccountId);
            cstmt.setLong(3, amount);

            cstmt.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Error transferring funds: " + e.getMessage());
            return false;
        }
    }

    // Loan methods
    public List<Loan> getLoansByCustomerId(Long customerId) {
        List<Loan> loans = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM loans WHERE cust_id = ?")) {

            pstmt.setLong(1, customerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(new Loan(
                            rs.getLong("loan_id"),
                            rs.getLong("cust_id"),
                            rs.getLong("amount"),
                            rs.getInt("branch_id")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting loans: " + e.getMessage());
        }

        return loans;
    }

    public boolean takeLoan(Long customerId, Long amount, Integer branchId) {
        // First check if customer has a loan account, create one if not
        boolean loanAccountExists = false;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT account_id FROM account WHERE customer_id = ? AND type = 'LOAN'")) {

            pstmt.setLong(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            loanAccountExists = rs.next();

            // If no loan account exists, create one
            if (!loanAccountExists) {
                System.out.println("No loan account found for customer " + customerId + ". Creating one.");
                try (PreparedStatement createStmt = conn.prepareStatement(
                        "INSERT INTO account (customer_id, balance, status, type) VALUES (?, 0, 'active', 'LOAN')")) {
                    createStmt.setLong(1, customerId);
                    createStmt.executeUpdate();
                    System.out.println("Loan account created for customer " + customerId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking/creating loan account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Now proceed with taking the loan
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall("{CALL TakeLoan(?, ?, ?)}")) {

            cstmt.setLong(1, customerId);
            cstmt.setLong(2, amount);
            cstmt.setInt(3, branchId);

            cstmt.execute();
            System.out.println("Loan successfully created for customer " + customerId + " with amount " + amount);
            return true;
        } catch (SQLException e) {
            System.err.println("Error taking loan: " + e.getMessage());

            // Check for specific error conditions from the stored procedure
            if (e.getMessage().contains("No LOAN account found")) {
                System.err.println("No loan account found for the customer despite our attempt to create one");
            } else if (e.getMessage().contains("Invalid customer ID")) {
                System.err.println("Invalid customer ID: " + customerId);
            }

            e.printStackTrace();
            return false;
        }
    }


    public List<Loan> getAllLoans() {
        List<Loan> loans = FXCollections.observableArrayList();
        String sql = "SELECT * FROM loans";

        System.out.println("Attempting to retrieve all loans...");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                count++;
                Loan loan = new Loan(
                        rs.getLong("loan_id"),
                        rs.getLong("cust_id"),
                        rs.getLong("amount"),
                        rs.getInt("branch_id")
                );
                loans.add(loan);
                System.out.println("Retrieved loan: ID=" + loan.getLoanId() +
                        ", Customer ID=" + loan.getCustId() +
                        ", Amount=" + loan.getAmount());
            }

            System.out.println("Total loans retrieved: " + count);

        } catch (SQLException e) {
            System.err.println("Error retrieving loans: " + e.getMessage());
            e.printStackTrace();
        }

        return loans;
    }




    public boolean deleteCustomer(Long customerId) {
        Connection conn = null;
        PreparedStatement accountStmt = null;
        PreparedStatement loanStmt = null;
        PreparedStatement customerStmt = null;
        PreparedStatement authStmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            String getUidSql = "SELECT uid FROM customers WHERE id = ?";
            PreparedStatement getUidStmt = conn.prepareStatement(getUidSql);
            getUidStmt.setLong(1, customerId);
            rs = getUidStmt.executeQuery();

            String uid = null;
            if (rs.next()) {
                uid = rs.getString("uid");
            } else {
                // Customer not found
                conn.rollback();
                return false;
            }

            String transactionSql = "DELETE FROM transactions WHERE fromID IN (SELECT account_id FROM account WHERE customer_id = ?) " +
                    "OR toID IN (SELECT account_id FROM account WHERE customer_id = ?)";
            PreparedStatement transStmt = conn.prepareStatement(transactionSql);
            transStmt.setLong(1, customerId);
            transStmt.setLong(2, customerId);
            transStmt.executeUpdate();


            String accountSql = "DELETE FROM account WHERE customer_id = ?";
            accountStmt = conn.prepareStatement(accountSql);
            accountStmt.setLong(1, customerId);
            accountStmt.executeUpdate();


            String loanSql = "DELETE FROM loans WHERE cust_id = ?";
            loanStmt = conn.prepareStatement(loanSql);
            loanStmt.setLong(1, customerId);
            loanStmt.executeUpdate();


            String customerSql = "DELETE FROM customers WHERE id = ?";
            customerStmt = conn.prepareStatement(customerSql);
            customerStmt.setLong(1, customerId);
            int customerRows = customerStmt.executeUpdate();


            if (uid != null) {
                String authSql = "DELETE FROM auth_users WHERE id = ?";
                authStmt = conn.prepareStatement(authSql);
                authStmt.setString(1, uid);
                authStmt.executeUpdate();
            }

            conn.commit();
            return customerRows > 0;

        } catch (SQLException e) {
            // If something goes wrong, roll back the transaction
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            System.err.println("Error deleting customer: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (accountStmt != null) accountStmt.close();
                if (loanStmt != null) loanStmt.close();
                if (customerStmt != null) customerStmt.close();
                if (authStmt != null) authStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto commit
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

}

