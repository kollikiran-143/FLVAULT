package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface ICICIService {

	public BSInfo parseICICI1(ParseBankStmtRequestDTO request);

	public BSInfo parseICICI2(ParseBankStmtRequestDTO request);

	public BSInfo parseICICI3(ParseBankStmtRequestDTO request);

	public BSInfo parseICICI4(ParseBankStmtRequestDTO request);

	public BSInfo parseICICI5(ParseBankStmtRequestDTO request);

}
