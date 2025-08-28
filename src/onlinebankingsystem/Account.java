package onlinebankingsystem;

public class Account {
    private Long accountId;
    private Long customerId;
    private Long balance;
    private String status;
    private String type;

    public Account(Long accountId, Long customerId, Long balance, String status, String type) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.status = status;
        this.type = type;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
