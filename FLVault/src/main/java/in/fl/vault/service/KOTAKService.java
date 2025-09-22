package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface KOTAKService {
	
	public BSInfo parseKOTAK1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseKOTAK2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseKOTAK3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;

	public BSInfo parseKOTAK4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;

	public BSInfo parseKOTAK5(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
}
