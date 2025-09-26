package in.fl.vault.response;

import java.util.List;

public class BSInfo {

	private String name;
	private String phone1;
	private String phone2;
	private String aadhaar;
	private String pan;
	private String email;
	private String dob;
	private String address;
	private String accountNo;
	private String ifsc;
	private String branch;
	private String accountType;
	private String startDate;
	private String enDate;
	private String nominee;
	private String stmntFormat;

	private List<Transaction> transactions;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone1() {
		return phone1;
	}

	public void setPhone1(String phone1) {
		this.phone1 = phone1;
	}

	public String getPhone2() {
		return phone2;
	}

	public void setPhone2(String phone2) {
		this.phone2 = phone2;
	}

	public String getAadhaar() {
		return aadhaar;
	}

	public void setAadhaar(String aadhaar) {
		this.aadhaar = aadhaar;
	}

	public String getPan() {
		return pan;
	}

	public void setPan(String pan) {
		this.pan = pan;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	public String getIfsc() {
		return ifsc;
	}

	public void setIfsc(String ifsc) {
		this.ifsc = ifsc;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEnDate() {
		return enDate;
	}

	public void setEnDate(String enDate) {
		this.enDate = enDate;
	}

	public String getNominee() {
		return nominee;
	}

	public void setNominee(String nominee) {
		this.nominee = nominee;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public String getStmntFormat() {
		return stmntFormat;
	}

	public void setStmntFormat(String stmntFormat) {
		this.stmntFormat = stmntFormat;
	}

	@Override
	public String toString() {
		return String.format(
				"BSInfo [name=%s, phone1=%s, phone2=%s, aadhaar=%s, pan=%s, email=%s, dob=%s, address=%s, accountNo=%s, ifsc=%s, branch=%s, accountType=%s, startDate=%s, enDate=%s, nominee=%s, stmntFormat=%s, transactions=%s]",
				name, phone1, phone2, aadhaar, pan, email, dob, address, accountNo, ifsc, branch, accountType,
				startDate, enDate, nominee, stmntFormat, transactions);
	}
	
	public String printWithoutTrxs() {
		return String.format(
				"BSInfo [name=%s, phone1=%s, phone2=%s, aadhaar=%s, pan=%s, email=%s, dob=%s, address=%s, accountNo=%s, ifsc=%s, branch=%s, accountType=%s, startDate=%s, enDate=%s, nominee=%s, stmntFormat=%s, transactions=%s]",
				name, phone1, phone2, aadhaar, pan, email, dob, address, accountNo, ifsc, branch, accountType,
				startDate, enDate, nominee, stmntFormat, transactions != null ? transactions.size() : 0);
	}
	
}
