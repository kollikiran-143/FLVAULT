package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface DBSService {
	
	public BSInfo parseDBS1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseDBS2(ParseBankStmtRequestDTO request);
	
	public BSInfo parseDBS3(ParseBankStmtRequestDTO request);
}
