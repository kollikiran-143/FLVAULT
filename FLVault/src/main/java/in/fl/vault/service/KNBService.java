package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface KNBService {
	
	public BSInfo parseKNB1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseKNB2(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseKNB3(ParseBankStmtRequestDTO request);

	BSInfo parseKNB4(ParseBankStmtRequestDTO request);
}
