package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface IDFCService {
	
	public BSInfo parseIDFC1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseIDFC2(ParseBankStmtRequestDTO request);
	
	public BSInfo parseIDFC3(ParseBankStmtRequestDTO request);
}
