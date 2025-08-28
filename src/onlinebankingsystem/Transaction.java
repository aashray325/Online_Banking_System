package onlinebankingsystem;

public class Transaction {
    private Long transId;
    private String type;
    private Long fromID;
    private Long toID;
    private Long amount;

    public Transaction(Long transId, String type, Long fromID, Long toID, Long amount) {
        this.transId = transId;
        this.type = type;
        this.fromID = fromID;
        this.toID = toID;
        this.amount = amount;
    }

    public Long getTransId() { return transId; }
    public void setTransId(Long transId) { this.transId = transId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getFromID() { return fromID; }
    public void setFromID(Long fromID) { this.fromID = fromID; }

    public Long getToID() { return toID; }
    public void setToID(Long toID) { this.toID = toID; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
