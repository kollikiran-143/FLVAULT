package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.ParseBankStmtResponseDTO;

public interface StmtService {

	public ParseBankStmtResponseDTO parseStatement(ParseBankStmtRequestDTO request);
}
