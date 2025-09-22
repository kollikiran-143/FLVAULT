package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface SBIService {
	
	public BSInfo parseSBI1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI2(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI3(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI4(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI5(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI6(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI7(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI8(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseSBI9(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseSBI10(ParseBankStmtRequestDTO request);
}
