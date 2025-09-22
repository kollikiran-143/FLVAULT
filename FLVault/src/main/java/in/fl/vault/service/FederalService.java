package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface FederalService {
	
	public BSInfo parseFederal1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseFederal2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
}
