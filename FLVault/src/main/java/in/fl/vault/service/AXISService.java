package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface AXISService {
	
	public BSInfo parseAXIS1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseAXIS2(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseAXIS3(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseAXIS4(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseAXIS5(ParseBankStmtRequestDTO request);

	public BSInfo parseAXIS6(ParseBankStmtRequestDTO request);

	public BSInfo parseAXIS7(ParseBankStmtRequestDTO request);

	public BSInfo parseAXIS8(ParseBankStmtRequestDTO request);
}
