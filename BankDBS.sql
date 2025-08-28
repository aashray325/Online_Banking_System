DROP DATABASE IF EXISTS online_banking_system;
CREATE DATABASE online_banking_system;
USE online_banking_system;

CREATE TABLE IF NOT EXISTS auth_users (
    id VARCHAR(36) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone INT UNIQUE,
    uid VARCHAR(36) UNIQUE,
    password VARCHAR(255) NOT NULL,
    FOREIGN KEY (uid) REFERENCES auth_users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS account (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    balance BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS loans (
    loan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cust_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    branch_id INT NULL,
    FOREIGN KEY (cust_id) REFERENCES customers(id)
);

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

DELIMITER //
CREATE PROCEDURE CreateNewCustomer(
    IN firstName VARCHAR(255),
    IN lastName VARCHAR(255),
    IN phoneNum INT,
    IN userUid VARCHAR(36),
    IN userPassword VARCHAR(255)
)
BEGIN
    INSERT INTO customers (first_name, last_name, phone, uid, password)
    VALUES (firstName, lastName, phoneNum, userUid, userPassword);
END //
DELIMITER ;

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
