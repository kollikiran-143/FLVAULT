package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface UCOService {
	
	public BSInfo parseUCO1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseUCO2(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseUCO3(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseUCO4(ParseBankStmtRequestDTO request);

	public BSInfo parseUCO5(ParseBankStmtRequestDTO request);
}
