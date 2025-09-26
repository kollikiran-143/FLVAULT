package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class PSIBServiceImpl implements PSIBService {

	private final static Logger log = Logger.getLogger(PSIBServiceImpl.class);

	@Override
	public BSInfo parsePSIB1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PSIBServiceImpl parsePSIB1 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankstatementInfo.setName(CommonUtils.extractField(pdfText, "Customer\\s*Name\\s*:\\s*(.*)Date").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractField(pdfText, "\\n\\s*Address\\s*:([\\s\\S]*?)\\n\\s*Transaction").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(PSIB\\w{7})"));
			bankstatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*(\\S*)"));
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Statement.*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
			String dateFormat = "dd/MM/yyyy";
			if (period != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bankstatementInfo.setTransactions(extractTransactionsPSIB_1(filepath, bankstatementInfo.getAccountNo(), dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in PSIBServiceImpl parsePSIB1: " + e);
		}
		log.info("Exiting PSIBServiceImpl parsePSIB1: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PSIBServiceImpl parsePSIB1 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parsePSIB2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PSIBServiceImpl parsePSIB2 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankstatementInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Name\\s*:(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo
					.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Customer\\s*Address[\\s\\S]*?)\\n\\s*Period", 100).replaceAll("Customer", "").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC.*?(PSIB\\w{7})"));
			bankstatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*:\\s*(\\S*)"));
			bankstatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Period\\s*(\\d{2}\\.\\d{2}\\.\\d{4}).*(\\d{2}\\.\\d{2}\\.\\d{4})");
			String dateFormat = "ddMMyyyy";
			if (period != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\.", ""), dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\.", ""), dateFormat));
			}
			bankstatementInfo.setTransactions(extractTransactionsPSIB_2(filepath, bankstatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in PSIBServiceImpl parsePSIB2: " + e);
		}
		log.info("Exiting PSIBServiceImpl parsePSIB2: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PSIBServiceImpl parsePSIB2 is: " + timeTaken);
		return bankstatementInfo;
	}

	private List<Transaction> extractTransactionsPSIB_1(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length == 7 && !data[0].equalsIgnoreCase("Transaction Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1].replaceAll("\\s+", " ").trim());
					transaction.setTxnId(data[2]);

					if (data[4] != null && !data[4].equalsIgnoreCase("-")) {
						String debit = data[4].replace("₹", "").trim();
						transaction.setDebit(debit);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
						transaction.setAmount(debit);
					} else {
						String credit = data[5].replace("₹", "").trim();
						transaction.setCredit(credit);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
						transaction.setAmount(credit);
					}
					transaction.setBalance(data[6].replace("₹", "").trim());
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsPSIB_2(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		String dateFormat = "dd/MM/yyyy";
		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length == 8 && !data[1].replaceAll("\\s+", " ").equalsIgnoreCase("Transaction Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2].replaceAll("\\s+", " ").trim());
					transaction.setTxnId(data[3]);

					if (data[5] != null && !data[5].equalsIgnoreCase("-")) {
						String debit = data[5].trim();
						transaction.setDebit(debit);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
						transaction.setAmount(debit);
					} else {
						String credit = data[6].trim();
						transaction.setCredit(credit);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
						transaction.setAmount(credit);
					}
					transaction.setBalance(data[7].trim());
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}
