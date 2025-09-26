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
public class IOBServiceImpl implements IOBService {

	private final static Logger log = Logger.getLogger(IOBServiceImpl.class);

	@Override
	public BSInfo parseIOB1(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IOBServiceImpl parseIOB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*-\\s*(.*?)\\s"));
			bankStatementInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "(\\s*Account\\s*Number\\s*-?\\s*\\S*[\\s\\S]*)?\\n\\s*Customer", 65)
					.replaceAll("Account\\s*Number\\s*-?\\s*\\S*\\s*", " ").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Scheme\\s*Code\\s*:\\s.*?-(.*?)-").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Address.*\\n([\\s\\S]*?)\\n\\s*Statement.*period", 65));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*CODE\\s*:\\s*(.*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*-\\s*\\d*\\s*(.*)").replaceAll("\\s+", " "));
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*.*\\s*from\\s*(.*)\\s*to\\s*(.*)");
			if (period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], "dd/MM/yyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], "dd/MM/yyyy"));
			}
			bankStatementInfo.setTransactions(extractTransactionsIOB_1(filePath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//			 e.printStackTrace();
			log.error("Error in IOBServiceImpl parseIOB1: " + e);
		}

		log.info("Exiting IOBServiceImpl parseIOB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IOBServiceImpl parseIOB1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseIOB2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IOBServiceImpl parseIOB2 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			String accountNo = CommonUtils.extractField(text, "Account.*No\\s*:(.*?)\\s{10}");
			String name = CommonUtils.extractMultiLinesField(text, "(\\s*Name\\s*of\\s*Customer\\s*:[\\s\\S]*?)\\n\\s*Contact\\s*No", 95).replaceAll("Name\\s*of\\s*Customer\\s*:", "")
					.replaceAll("\\s+", " ").trim();
			accountNo = accountNo.replaceAll("[A-Za-z\s,.]+", "").trim();
			if (accountNo == null || accountNo.equalsIgnoreCase("") || accountNo.length() < 5) {
				accountNo = CommonUtils.extractField(text, "Branch\\s*Address.*?\\n\\s*:\\s*(\\S+)\\s*\\n\\s*Account\\s*No");
			}
			if (name == null || name.trim().equalsIgnoreCase("")) {
				name = CommonUtils.extractMultiLinesField(text, "\\n(\\s*Account\\s*No\\s*:[\\s\\S]*?)\\n\\s*Contact\\s*No", 50, 35);
				name = name.replaceAll("\\s+|:", " ").trim();
			}

			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(name);
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Contact\\s*No\\s*:(.*?)\\s{5}"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*ID\\s*:(.*?)\\s{10}"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*Code[\\s\\S]*?(IOBA\\w{7})"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(\\w*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Address\\s*of\\s*Customer.*\\n([\\s\\S]*?)\\n\\s*Date.*Particulars", 95, 175));

			List<Transaction> transactions = extractTransactionsIOB_2(filepath, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
			bankStatementInfo.setStartDate(transactions.get(0).getTxnDate());
			bankStatementInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());

		} catch (Exception e) {
//			 e.printStackTrace();
			log.error("Error in IOBServiceImpl parseIOB2: ", e);
		}

		log.info("Exiting IOBServiceImpl parseIOB2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IOBServiceImpl parseIOB2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseIOB3(ParseBankStmtRequestDTO request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BSInfo parseIOB4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IOBServiceImpl parseIOB4 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "A\\/C\\s*NO\\s*:\\s*(\\S*)\\s*INR"));
			bankStatementInfo.setName(CommonUtils.extractField(text, "PAGE\\s*:.*\\n(.*)\\n").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "TYPE\\s*:(.*)").replaceAll("\\s+", " ").trim());
			String address = CommonUtils.extractField(text, "PAGE.*?\\n.*?\\n\\s*([\\s\\S]*?)\\n\\s*STATEMENT");
			bankStatementInfo.setAddress(address.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());

			String dateFormat = "dd-MM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s*OF\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s*OF.*?to\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));

			List<Transaction> transactions = extractTransactionsIOB_4(text, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			 e.printStackTrace();
			log.error("Error in IOBServiceImpl parseIOB4: ", e);
		}

		log.info("Exiting IOBServiceImpl parseIOB4: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IOBServiceImpl parseIOB4 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsIOB_4(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile("\\s+(\\d{2}-\\d{2}-\\d{4})\\s*(.*?)\\s+([\\d,]+\\.\\d{2})\\wr(\\s+)([\\d,]+\\.\\d{2})");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription(matcher.group(2).replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 25) {
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(transaction.getAmount());
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsIOB_1(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty() || row.length <= 4) {
					continue;
				}

				String[] data = line.split("\\|");

				if (data.length >= 6 && !data[0].equals("DATE")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MMM-yyyy"));
					transaction.setDescription(data[2]);
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);

					if (!data[4].isEmpty() && !data[4].equals("-")) {
						transaction.setAmount(data[4]);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[5]);
						transaction.setTxnType("CREDIT");
					}

					transaction.setBalance(data[6]);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsIOB_2(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		Pattern datePattern1 = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{4}");
		Pattern datePattern2 = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{2}");
		String dateFormat = "";

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}

				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[1].contains("Particulars")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					String txnDate = CommonUtils.extractField(data[0], "(.*)\\(.*\\)");
					String valueDate = CommonUtils.extractField(data[0], ".*\\((.*)\\)");
					if (dateFormat.equalsIgnoreCase("")) {
						Matcher dateMatcher1 = datePattern1.matcher(txnDate);
						Matcher dateMatcher2 = datePattern2.matcher(txnDate);
						if (dateMatcher1.find()) {
							dateFormat = "dd-MMM-yyyy";
						} else if (dateMatcher2.find()) {
							dateFormat = "dd-MMM-yy";
						}
					}

					transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(valueDate, dateFormat));
					transaction.setDescription(data[1]);

					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5] == null || data[5].equals("-")) {
						transaction.setCredit("");
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setDebit("");
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[6]);

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}