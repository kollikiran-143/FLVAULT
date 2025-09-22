package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface INDALHService {
	
	public BSInfo parseINDALH1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseINDALH2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseINDALH3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseINDALH4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
}
