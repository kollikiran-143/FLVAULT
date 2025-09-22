package in.fl.vault.response;

import java.util.List;

public class ParseBankStmtResponseDTO extends BaseResponseDTO{
	
	private List<BSInfo> bankStatements;

	public List<BSInfo> getBankStatements() {
		return bankStatements;
	}

	public void setBankStatements(List<BSInfo> bankStatements) {
		this.bankStatements = bankStatements;
	}

	@Override
	public String toString() {
		return String.format(
				"ParseBankStmtResponseDTO [bankStatements=%s, getStatusCode()=%s, getStatusMessage()=%s, getRandom()=%s]",
				bankStatements, getStatusCode(), getStatusMessage(), getRandom());
	}

}
