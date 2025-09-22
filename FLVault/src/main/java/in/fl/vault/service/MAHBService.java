package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface MAHBService {
	
	public BSInfo parseMAHB1(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseMAHB2(ParseBankStmtRequestDTO request) throws IOException;
}
