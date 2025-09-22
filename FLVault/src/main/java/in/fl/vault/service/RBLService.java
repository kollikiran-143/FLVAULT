package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface RBLService {
	
	public BSInfo parseRBL1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseRBL2(ParseBankStmtRequestDTO request);

	public BSInfo parseRBL3(ParseBankStmtRequestDTO request);

	BSInfo parseRBL4(ParseBankStmtRequestDTO request);
	
}
