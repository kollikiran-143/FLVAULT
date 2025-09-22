package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface CanaraService {
	public BSInfo parseCanara1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCanara4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCanara5(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCanara6(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseCanara7(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
}
