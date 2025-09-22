package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface KVBService {
	
	public BSInfo parseKVB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseKVB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;

	BSInfo parseKVB3(ParseBankStmtRequestDTO request);
}
