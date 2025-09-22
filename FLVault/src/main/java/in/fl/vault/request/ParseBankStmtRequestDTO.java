package in.fl.vault.request;

public class ParseBankStmtRequestDTO extends BaseRequestDTO{

	private String fileName;
	private String password;
	private String bankCode;
	private String bankName;
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	@Override
	public String toString() {
		return "ParseBankStmtRequestDTO [fileName=" + fileName + ", password=" + password + ", bankCode=" + bankCode
				+ ", bankName=" + bankName + ", getAppName()=" + getAppName() + ", getCustomerId()=" + getCustomerId()
				+ ", getRandom()=" + getRandom() + "]";
	}
}
