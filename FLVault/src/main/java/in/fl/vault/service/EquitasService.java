package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface EquitasService {
	
	public BSInfo parseEquitas1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseEquitas2(ParseBankStmtRequestDTO request);
	
	public BSInfo parseEquitas3(ParseBankStmtRequestDTO request);
	
	public BSInfo parseEquitas4(ParseBankStmtRequestDTO request);
}
