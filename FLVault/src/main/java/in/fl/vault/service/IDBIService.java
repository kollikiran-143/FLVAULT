package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface IDBIService {
	
	public BSInfo parseIDBI1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIDBI2(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIDBI3(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIDBI4(ParseBankStmtRequestDTO request) throws IOException;
}
