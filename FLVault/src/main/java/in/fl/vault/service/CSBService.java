package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface CSBService {
	
	public BSInfo parseCSB1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseCSB2(ParseBankStmtRequestDTO request);
	
	public BSInfo parseCSB3(ParseBankStmtRequestDTO request);

	BSInfo parseCSB4(ParseBankStmtRequestDTO request);

	public BSInfo parseCSB5(ParseBankStmtRequestDTO request);

	BSInfo parseCSB6(ParseBankStmtRequestDTO request);

	BSInfo parseCSB7(ParseBankStmtRequestDTO request);

	public BSInfo parseCSB8(ParseBankStmtRequestDTO request);
}
