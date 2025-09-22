package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface TMBService {
	
	public BSInfo parseTMB1(ParseBankStmtRequestDTO request) throws IOException; 
	
	public BSInfo parseTMB2(ParseBankStmtRequestDTO request) throws IOException; 
	
	public BSInfo parseTMB3(ParseBankStmtRequestDTO request);
	
	public BSInfo parseTMB4(ParseBankStmtRequestDTO request) throws IOException;

	BSInfo parseTMB5(ParseBankStmtRequestDTO request);

	BSInfo parseTMB6(ParseBankStmtRequestDTO request);

	BSInfo parseTMB7(ParseBankStmtRequestDTO request) throws IOException;
}
