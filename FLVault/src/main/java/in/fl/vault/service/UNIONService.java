package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface UNIONService {

	public BSInfo parseUNION1(ParseBankStmtRequestDTO request);

	public BSInfo parseUNION2(ParseBankStmtRequestDTO request);

	public BSInfo parseUNION3(ParseBankStmtRequestDTO request);

	public BSInfo parseUNION4(ParseBankStmtRequestDTO request);

	public BSInfo parseUNION5(ParseBankStmtRequestDTO request);
}
