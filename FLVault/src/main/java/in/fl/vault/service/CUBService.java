package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface CUBService {
	
	public BSInfo parseCUB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCUB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCUB3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
}
