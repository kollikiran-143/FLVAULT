package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class UJVNServiceImpl implements UJVNService{
	
	private final static Logger log = Logger.getLogger(UJVNServiceImpl.class);

	@Override
	public BSInfo parseUJVN1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UJVNServiceImpl parseUJVN1 with request: " + request);

		String filepath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(UJVN\\w{7})"));
			bankStatementInfo.setName(
					CommonUtils.extractMultiLinesField(text, "(\\s*Name\\s*:[\\s\\S]*?)(?=\\s*Address)", 30, 45)
							.replaceAll("\\s+", " ").replaceAll(":", "").trim());
			String nomine = CommonUtils.extractField(text, "Nomination\\s*:\\s*(.{40})").replaceAll("\\s+", " ").trim();
			bankStatementInfo.setNominee(nomine);

			String address = CommonUtils.extractMultiLinesField(text,
					"(\\s*Address[\\s\\S]*?)\\n\\s*Nomination", 30, 45).replaceAll(":", "");
			bankStatementInfo.setAddress(address);
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Scheme\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			String dateFormat = "dd-MM-yy";
			bankStatementInfo
					.setTransactions(extracTransactionsUJVN_1(filepath, bankStatementInfo.getAccountNo(), dateFormat));

			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(bankStatementInfo.getTransactions().get(0).getTxnDate(), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(bankStatementInfo.getTransactions()
					.get(bankStatementInfo.getTransactions().size() - 1).getTxnDate(), dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in UJVNServiceImpl parseUJVN1: " + e);
		}
		log.info("Exiting UJVNServiceImpl parseUJVN1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UJVNServiceImpl parseUJVN1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extracTransactionsUJVN_1(String fileName, String accountNo, String dateFormat)
			throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				Pattern pattern = Pattern.compile("\\d{2}-\\d{2}-\\d{2}");
				Matcher matcher = pattern.matcher(data[0]);
				if (!matcher.find()) {
					continue;
				}

				if (data.length >= 6 && !data[0].equalsIgnoreCase("Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);

					if (!data[3].isEmpty()) {
						transaction.setAmount(data[3]);
						transaction.setDebit(data[3]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[4]);
						transaction.setCredit(data[4]);
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					}
					transaction.setBalance(data[5]);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}


