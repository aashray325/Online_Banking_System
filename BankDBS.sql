CREATE DATABASE IF NOT EXISTS online_banking_system;
USE online_banking_system;

-- Create auth_users table
CREATE TABLE IF NOT EXISTS auth_users (
    id VARCHAR(36) PRIMARY KEY
);

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone INT UNIQUE,
    uid VARCHAR(36) UNIQUE,
    FOREIGN KEY (uid) REFERENCES auth_users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Create account table
CREATE TABLE IF NOT EXISTS account (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    balance BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Create loans table
CREATE TABLE IF NOT EXISTS loans (
    loan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cust_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    branch_id INT NULL,
    FOREIGN KEY (cust_id) REFERENCES customers(id)
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    trans_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    fromID BIGINT NULL,
    toID BIGINT NULL,
    amount BIGINT NOT NULL,
    UNIQUE (trans_id),
    FOREIGN KEY (fromID) REFERENCES account(account_id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (toID) REFERENCES account(account_id) ON UPDATE CASCADE ON DELETE CASCADE
);

USE online_banking_system;

-- Insert sample data into auth_users table
INSERT INTO auth_users (id) VALUES ('uid1');
INSERT INTO auth_users (id) VALUES ('uid2');
INSERT INTO auth_users (id) VALUES ('uid3');

-- Insert sample data into customers table
INSERT INTO customers (id, first_name, last_name, phone, uid) VALUES (1, 'John', 'Doe', 1234567890, 'uid1');
INSERT INTO customers (id, first_name, last_name, phone, uid) VALUES (2, 'Jane', 'Smith', 9876543210, 'uid2');
INSERT INTO customers (id, first_name, last_name, phone, uid) VALUES (3, 'Alice', 'Brown', 1122334455, 'uid3');

-- Insert sample data into account table
INSERT INTO account (account_id, customer_id, balance, status, type) VALUES (101, 1, 5000, 'active', 'savings');
INSERT INTO account (account_id, customer_id, balance, status, type) VALUES (102, 2, 10000, 'active', 'checking');
INSERT INTO account (account_id, customer_id, balance, status, type) VALUES (103, 3, 7500, 'inactive', 'loan');

-- Insert sample data into loans table
INSERT INTO loans (loan_id, cust_id, amount, branch_id) VALUES (201, 1, 20000, 1);
INSERT INTO loans (loan_id, cust_id, amount, branch_id) VALUES (202, 2, 15000, 2);

-- Insert sample data into transactions table
INSERT INTO transactions (trans_id, type, fromID, toID, amount) VALUES (301, 'transfer', 101, 102, 1000);
INSERT INTO transactions (trans_id, type, fromID, toID, amount) VALUES (302, 'deposit', NULL, 101, 2000);

USE online_banking_system;

-- Stored Procedure for TransferFunds
DELIMITER //
CREATE PROCEDURE TransferFunds(
    IN fromAccountId BIGINT,
    IN toAccountId BIGINT,
    IN transferAmount BIGINT
)
BEGIN
    START TRANSACTION;

    SELECT balance INTO @fromBalance FROM account WHERE account_id = fromAccountId FOR UPDATE;
    IF @fromBalance IS NULL OR @fromBalance < transferAmount THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient balance or invalid source account.';
    END IF;

    SELECT account_id INTO @toAccount FROM account WHERE account_id = toAccountId;
    IF @toAccount IS NULL THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid destination account.';
    END IF;

    UPDATE account SET balance = balance - transferAmount WHERE account_id = fromAccountId;
    UPDATE account SET balance = balance + transferAmount WHERE account_id = toAccountId;

    INSERT INTO transactions (type, fromID, toID, amount)
    VALUES ('transfer', fromAccountId, toAccountId, transferAmount);

    COMMIT;
END //
DELIMITER ;

-- Stored Procedure for CreateNewCustomer
DELIMITER //
CREATE PROCEDURE CreateNewCustomer(
    IN firstName VARCHAR(255),
    IN lastName VARCHAR(255),
    IN phoneNum INT,
    IN userUid VARCHAR(36)
)
BEGIN
    INSERT INTO customers (first_name, last_name, phone, uid)
    VALUES (firstName, lastName, phoneNum, userUid);
END //
DELIMITER ;

-- Stored Procedure for TakeLoan
DELIMITER //
CREATE PROCEDURE TakeLoan(
    IN customerId BIGINT,
    IN loanAmount BIGINT,
    IN branchId INT
)
BEGIN
    SELECT id INTO @customerIdExists FROM customers WHERE id = customerId;
    IF @customerIdExists IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid customer ID.';
    END IF;

    INSERT INTO loans (cust_id, amount, branch_id)
    VALUES (customerId, loanAmount, branchId);

    SELECT account_id INTO @loanAccount FROM account WHERE customer_id = customerId AND type = 'LOAN';
    IF @loanAccount IS NOT NULL THEN
        UPDATE account SET balance = balance + loanAmount WHERE account_id = @loanAccount;
        INSERT INTO transactions (type, toID, amount) VALUES ('loan_deposit', @loanAccount, loanAmount);
    ELSE
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No LOAN account found for the customer.';
    END IF;
END //
DELIMITER ;

-- Function to Get Account Balance
DELIMITER //
CREATE FUNCTION GetAccountBalance(accountId BIGINT)
RETURNS BIGINT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE currentBalance BIGINT;
    SELECT balance INTO currentBalance FROM account WHERE account_id = accountId;
    RETURN currentBalance;
END //
DELIMITER ;
