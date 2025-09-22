package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface IOBService {
	
	public BSInfo parseIOB1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIOB2(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseIOB3(ParseBankStmtRequestDTO request);
}
