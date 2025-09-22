package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface YESBService {
	
	public BSInfo parseYESB1(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseYESB2(ParseBankStmtRequestDTO request);

	public BSInfo parseYESB3(ParseBankStmtRequestDTO request);
}
