package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface CNTBService {
	
	public BSInfo parseCNTB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCNTB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCNTB3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCNTB4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
}
