package onlinebankingsystem;

public class Loan {
    private Long loanId;
    private Long custId;
    private Long amount;
    private Integer branchId;

    public Loan(Long loanId, Long custId, Long amount, Integer branchId) {
        this.loanId = loanId;
        this.custId = custId;
        this.amount = amount;
        this.branchId = branchId;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
}
