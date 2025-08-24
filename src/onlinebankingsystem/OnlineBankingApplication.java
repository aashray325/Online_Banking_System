package onlinebankingsystem;

import javafx.application.Application;
import javafx.collections.FXCollections;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;


public class OnlineBankingApplication extends Application {

    // UI Components
    private Stage primaryStage;
    private TableView<Customer> customerTable;
    private TableView<Account> accountTable;
    private TableView<Transaction> transactionTable;
    private TableView<Loan> loanTable;

    // Current customer ID for operations
    private Long currentCustomerId;

    // Database service
    private DatabaseService dbService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.dbService = new DatabaseService();

        // Start with login screen
        showLoginScreen();

        primaryStage.setTitle("Online Banking System");
        primaryStage.show();
    }

    // ==================== UI SCREENS ====================

    private void showSignUpForm() {
        SignUpForm signUpForm = new SignUpForm(dbService);
        signUpForm.setVisible(true);
    }

    private void showLoginScreen() {
        // Create simple login for demo purposes (admin and customer)
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label titleLabel = new Label("Online Banking System");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        grid.add(titleLabel, 0, 0, 2, 1);

        Button adminBtn = new Button("Admin Login");
        adminBtn.setOnAction(e -> showAdminDashboard());

        ComboBox<Customer> customerSelect = new ComboBox<>();
        List<Customer> customers = dbService.getAllCustomers();
        ObservableList<Customer> customerOptions = FXCollections.observableArrayList(customers);
        customerSelect.setItems(customerOptions);
        customerSelect.setPromptText("Select Customer");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");


        customerSelect.setCellFactory(cell -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getFullName());
                }
            }
        });

        customerSelect.setButtonCell(new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getFullName());
                }
            }
        });



        Button customerBtn = new Button("Customer Login");
        customerBtn.setOnAction(e -> {
            Customer selected = customerSelect.getValue();
            String password = passwordField.getText();
            if (selected == null) {
                showAlert("Please select a customer");
            } else if (password.isEmpty()) {
                showAlert("Please enter your password");
            } else if (verifyCustomerPassword(selected, password)) {
                showCustomerDashboard(selected);
            } else {
                showAlert("Invalid password");
            }
        });


        Button signUpButton = new Button("Sign Up");
        signUpButton.setOnAction(e -> showSignUpForm());


        VBox loginBox = new VBox(10);
        loginBox.getChildren().addAll(
                new Label("Admin:"), adminBtn,
                new Label("Customer:"), customerSelect, passwordField, customerBtn,
                new Label("New User:"), signUpButton
        );

        grid.add(loginBox, 0, 1);

        Scene scene = new Scene(grid, 400, 300);
        primaryStage.setScene(scene);
    }

    private boolean verifyCustomerPassword(Customer customer, String password) {
        return password.equals(customer.getPassword());
    }


    private void showCustomerDashboard(Customer customer) {
        currentCustomerId = customer.getId();

        TabPane tabPane = new TabPane();

        // Accounts Tab
        Tab accountsTab = new Tab("Accounts");
        accountsTab.setClosable(false);

        accountTable = new TableView<>();

        TableColumn<Account, Long> accountIdCol = new TableColumn<>("Account ID");
        accountIdCol.setCellValueFactory(new PropertyValueFactory<>("accountId"));

        TableColumn<Account, String> accountTypeCol = new TableColumn<>("Type");
        accountTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Account, Long> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));

        TableColumn<Account, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        accountTable.getColumns().addAll(accountIdCol, accountTypeCol, balanceCol, statusCol);

        // Load customer accounts
        List<Account> accounts = dbService.getAccountsByCustomerId(customer.getId());
        accountTable.setItems(FXCollections.observableArrayList(accounts));

        // Transfer money form
        GridPane transferForm = new GridPane();
        transferForm.setHgap(10);
        transferForm.setVgap(10);
        transferForm.setPadding(new Insets(10));

        ComboBox<Account> fromAccountCombo = new ComboBox<>();
        fromAccountCombo.setItems(FXCollections.observableArrayList(accounts));
        fromAccountCombo.setPromptText("From Account");

        TextField toAccountField = new TextField();
        toAccountField.setPromptText("To Account ID");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        Button transferBtn = new Button("Transfer");
        transferBtn.setOnAction(e -> {
            try {
                Account fromAccount = fromAccountCombo.getValue();
                Long toAccountId = Long.parseLong(toAccountField.getText());
                Long amount = Long.parseLong(amountField.getText());

                if (fromAccount != null && toAccountId != null && amount > 0) {
                    boolean success = dbService.transferFunds(fromAccount.getAccountId(), toAccountId, amount);
                    if (success) {
                        showAlert("Transfer successful!");
                        // Refresh accounts
                        List<Account> updatedAccounts = dbService.getAccountsByCustomerId(customer.getId());
                        accountTable.setItems(FXCollections.observableArrayList(updatedAccounts));
                    } else {
                        showAlert("Transfer failed!");
                    }
                } else {
                    showAlert("Please fill all fields correctly");
                }
            } catch (NumberFormatException ex) {
                showAlert("Invalid account ID or amount");
            }
        });

        transferForm.add(new Label("Transfer Money:"), 0, 0, 2, 1);
        transferForm.add(new Label("From Account:"), 0, 1);
        transferForm.add(fromAccountCombo, 1, 1);
        transferForm.add(new Label("To Account:"), 0, 2);
        transferForm.add(toAccountField, 1, 2);
        transferForm.add(new Label("Amount:"), 0, 3);
        transferForm.add(amountField, 1, 3);
        transferForm.add(transferBtn, 1, 4);

        VBox accountsBox = new VBox(10);
        accountsBox.setPadding(new Insets(10));
        accountsBox.getChildren().addAll(
                new Label("Accounts for " + customer.getFullName()),
                accountTable,
                new Separator(),
                transferForm
        );

        accountsTab.setContent(accountsBox);

        // Transactions Tab
        Tab transactionsTab = new Tab("Transactions");
        transactionsTab.setClosable(false);

        ComboBox<Account> accountCombo = new ComboBox<>();
        accountCombo.setItems(FXCollections.observableArrayList(accounts));
        accountCombo.setPromptText("Select Account");

        transactionTable = new TableView<>();

        TableColumn<Transaction, Long> transIdCol = new TableColumn<>("Transaction ID");
        transIdCol.setCellValueFactory(new PropertyValueFactory<>("transId"));

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Transaction, Long> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("fromID"));

        TableColumn<Transaction, Long> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(new PropertyValueFactory<>("toID"));

        TableColumn<Transaction, Long> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        transactionTable.getColumns().addAll(transIdCol, typeCol, fromCol, toCol, amountCol);

        Button viewTransactionsBtn = new Button("View Transactions");
        viewTransactionsBtn.setOnAction(e -> {
            Account selected = accountCombo.getValue();
            if (selected != null) {
                List<Transaction> transactions = dbService.getTransactionsByAccountId(selected.getAccountId());
                transactionTable.setItems(FXCollections.observableArrayList(transactions));
            } else {
                showAlert("Please select an account");
            }
        });

        VBox transactionsBox = new VBox(10);
        transactionsBox.setPadding(new Insets(10));
        transactionsBox.getChildren().addAll(
                new Label("Transactions"),
                new HBox(10, accountCombo, viewTransactionsBtn),
                transactionTable
        );

        transactionsTab.setContent(transactionsBox);

        // Loans Tab
        Tab loansTab = new Tab("Loans");
        loansTab.setClosable(false);

        loanTable = new TableView<>();

        TableColumn<Loan, Long> loanIdCol = new TableColumn<>("Loan ID");
        loanIdCol.setCellValueFactory(new PropertyValueFactory<>("loanId"));

        TableColumn<Loan, Long> loanAmountCol = new TableColumn<>("Amount");
        loanAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Loan, Integer> branchCol = new TableColumn<>("Branch");
        branchCol.setCellValueFactory(new PropertyValueFactory<>("branchId"));

        loanTable.getColumns().addAll(loanIdCol, loanAmountCol, branchCol);

        // Load customer loans
        List<Loan> loans = dbService.getLoansByCustomerId(customer.getId());
        loanTable.setItems(FXCollections.observableArrayList(loans));

        // Apply for loan form
        GridPane loanForm = new GridPane();
        loanForm.setHgap(10);
        loanForm.setVgap(10);
        loanForm.setPadding(new Insets(10));

        TextField loanAmountField = new TextField();
        loanAmountField.setPromptText("Loan Amount");

        TextField branchIdField = new TextField();
        branchIdField.setPromptText("Branch ID");

        Button applyBtn = new Button("Apply for Loan");
        applyBtn.setOnAction(e -> {
            try {
                Long amount = Long.parseLong(loanAmountField.getText());
                Integer branchId = Integer.parseInt(branchIdField.getText());

                if (amount > 0) {
                    boolean success = dbService.takeLoan(customer.getId(), amount, branchId);
                    if (success) {
                        showAlert("Loan application successful!");
                        // Refresh loans
                        List<Loan> updatedLoans = dbService.getLoansByCustomerId(customer.getId());
                        loanTable.setItems(FXCollections.observableArrayList(updatedLoans));
                    } else {
                        showAlert("Loan application failed!");
                    }
                } else {
                    showAlert("Please enter a valid amount");
                }
            } catch (NumberFormatException ex) {
                showAlert("Invalid loan amount or branch ID");
            }
        });

        loanForm.add(new Label("Apply for Loan:"), 0, 0, 2, 1);
        loanForm.add(new Label("Amount:"), 0, 1);
        loanForm.add(loanAmountField, 1, 1);
        loanForm.add(new Label("Branch ID:"), 0, 2);
        loanForm.add(branchIdField, 1, 2);
        loanForm.add(applyBtn, 1, 3);

        VBox loansBox = new VBox(10);
        loansBox.setPadding(new Insets(10));
        loansBox.getChildren().addAll(
                new Label("Loans for " + customer.getFullName()),
                loanTable,
                new Separator(),
                loanForm
        );

        loansTab.setContent(loansBox);

        // Add tabs to tab pane
        tabPane.getTabs().addAll(accountsTab, transactionsTab, loansTab);

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginScreen());

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new HBox(10, new Label("Welcome, " + customer.getFullName()), logoutBtn));
        borderPane.setCenter(tabPane);

        Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Customer Dashboard - " + customer.getFullName());
    }

    private void showAdminDashboard() {
        BorderPane borderPane = new BorderPane();
        TabPane tabPane = new TabPane();

        // ========== CUSTOMERS TAB ==========
        Tab customersTab = new Tab("Customers");
        customersTab.setClosable(false);

        customerTable = new TableView<>();

        TableColumn<Customer, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Customer, String> firstNameCol = new TableColumn<>("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<Customer, String> lastNameCol = new TableColumn<>("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<Customer, Integer> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> passwordCol = new TableColumn<>("Password");
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));


        customerTable.getColumns().addAll(idCol, firstNameCol, lastNameCol, phoneCol, passwordCol);

        // Load all customers
        List<Customer> customers = dbService.getAllCustomers();
        customerTable.setItems(FXCollections.observableArrayList(customers));

        Button refreshBtn = new Button("Refresh Customers");
        refreshBtn.setOnAction(e -> {
            List<Customer> refreshedCustomers = dbService.getAllCustomers();
            customerTable.setItems(FXCollections.observableArrayList(refreshedCustomers));
        });

        // Add delete customer button
        Button deleteCustomerBtn = new Button("Delete Selected Customer");
        deleteCustomerBtn.setOnAction(e -> {
            Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                deleteCustomer(selectedCustomer);
            } else {
                showAlert("Please select a customer to delete");
            }
        });

        HBox customerButtons = new HBox(10, refreshBtn, deleteCustomerBtn);

        VBox customersBox = new VBox(10);
        customersBox.setPadding(new Insets(10));
        customersBox.getChildren().addAll(
                new Label("All Customers"),
                customerTable,
                customerButtons
        );

        customersTab.setContent(customersBox);

        // ========== LOANS TAB ==========
        Tab loansTab = new Tab("Loans Management");
        loansTab.setClosable(false);

        // Create table for loans with customer info
        TableView<LoanWithCustomerInfo> loanTable = new TableView<>();

        TableColumn<LoanWithCustomerInfo, Long> loanIdCol = new TableColumn<>("Loan ID");
        loanIdCol.setCellValueFactory(new PropertyValueFactory<>("loanId"));

        TableColumn<LoanWithCustomerInfo, String> customerNameCol = new TableColumn<>("Customer Name");
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<LoanWithCustomerInfo, Long> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<LoanWithCustomerInfo, Integer> branchIdCol = new TableColumn<>("Branch ID");
        branchIdCol.setCellValueFactory(new PropertyValueFactory<>("branchId"));

        loanTable.getColumns().addAll(loanIdCol, customerNameCol, amountCol, branchIdCol);

        // Load loans with customer info
        List<LoanWithCustomerInfo> loansWithInfo = getAllLoansWithCustomerInfo();
        loanTable.setItems(FXCollections.observableArrayList(loansWithInfo));

        Button refreshLoansBtn = new Button("Refresh Loans");
        refreshLoansBtn.setOnAction(e -> {
            List<LoanWithCustomerInfo> refreshedLoans = getAllLoansWithCustomerInfo();
            loanTable.setItems(FXCollections.observableArrayList(refreshedLoans));
        });

        VBox loansBox = new VBox(10);
        loansBox.setPadding(new Insets(10));
        loansBox.getChildren().addAll(
                new Label("All Loans"),
                loanTable,
                refreshLoansBtn
        );

        loansTab.setContent(loansBox);

        // Add tabs to tab pane
        tabPane.getTabs().addAll(customersTab, loansTab);

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginScreen());

        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.getChildren().addAll(new Label("Admin Dashboard"), logoutBtn);

        borderPane.setTop(topBox);
        borderPane.setCenter(tabPane);

        Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Admin Dashboard");
    }

    // Class to hold loan information with customer details
    private static class LoanWithCustomerInfo {
        private Long loanId;
        private Long customerId;
        private String customerName;
        private Long amount;
        private Integer branchId;

        public LoanWithCustomerInfo(Long loanId, Long customerId, String customerName, Long amount, Integer branchId) {
            this.loanId = loanId;
            this.customerId = customerId;
            this.customerName = customerName;
            this.amount = amount;
            this.branchId = branchId;
        }

        // Getters (needed for TableView)
        public Long getLoanId() { return loanId; }
        public Long getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public Long getAmount() { return amount; }
        public Integer getBranchId() { return branchId; }
    }

    // Helper method to get all loans with customer information
    private List<LoanWithCustomerInfo> getAllLoansWithCustomerInfo() {
        List<LoanWithCustomerInfo> result = FXCollections.observableArrayList();

        // Get all loans from database with debugging
        System.out.println("Fetching all loans for manager UI...");
        List<Loan> loansList = dbService.getAllLoans();
        System.out.println("Retrieved " + loansList.size() + " loans to process");

        // For each loan, get customer info
        for (Loan loan : loansList) {
            Customer customer = dbService.getCustomerById(loan.getCustId());
            if (customer != null) {
                String customerName = customer.getFirstName() + " " + customer.getLastName();
                LoanWithCustomerInfo loanInfo = new LoanWithCustomerInfo(
                        loan.getLoanId(),
                        loan.getCustId(),
                        customerName,
                        loan.getAmount(),
                        loan.getBranchId()
                );
                result.add(loanInfo);
                System.out.println("Added loan with customer info: " + loanInfo.getLoanId() +
                        ", Customer: " + customerName);
            } else {
                System.out.println("WARNING: Could not find customer with ID " + loan.getCustId() +
                        " for loan " + loan.getLoanId());
            }
        }

        System.out.println("Returning " + result.size() + " loans with customer info");
        return result;
    }




    // Method to delete a customer
    private void deleteCustomer(Customer customer) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Customer");
        confirmAlert.setContentText("Are you sure you want to delete customer " +
                customer.getFirstName() + " " + customer.getLastName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = dbService.deleteCustomer(customer.getId());
                if (success) {
                    customerTable.getItems().remove(customer);
                    showAlert("Customer deleted successfully");
                } else {
                    showAlert("Failed to delete customer. They may have active accounts or loans.");
                }
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



