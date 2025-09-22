package in.fl.vault.response;

public class Transaction {

	private String sNo;
	private String txnDate;
	private String description;
	private String txnId;
	private String debit;
	private String credit;
	private String balance;
	private String txnType;
	private String accNo;
	private String amount;
	private String valueDate;

	public String getsNo() {
		return sNo;
	}

	public void setsNo(String sNo) {
		this.sNo = sNo;
	}

	public String getTxnDate() {
		return txnDate;
	}

	public void setTxnDate(String txnDate) {
		this.txnDate = txnDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public String getDebit() {
		return debit;
	}

	public void setDebit(String debit) {
		this.debit = debit;
	}

	public String getCredit() {
		return credit;
	}

	public void setCredit(String credit) {
		this.credit = credit;
	}

	public String getBalance() {
		return balance;
	}

	public void setBalance(String balance) {
		this.balance = balance;
	}

	public String getTxnType() {
		return txnType;
	}

	public void setTxnType(String txnType) {
		this.txnType = txnType;
	}

	public String getAccNo() {
		return accNo;
	}

	public void setAccNo(String accNo) {
		this.accNo = accNo;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getValueDate() {
		return valueDate;
	}

	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}

	@Override
	public String toString() {
		return String.format(
				"Transaction [sNo=%s, txnDate=%s, description=%s, txnId=%s, debit=%s, credit=%s, balance=%s, txnType=%s, accNo=%s, amount=%s, valueDate=%s]",
				sNo, txnDate, description, txnId, debit, credit, balance, txnType, accNo, amount, valueDate);
	}

}
