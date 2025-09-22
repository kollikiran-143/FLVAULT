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
public class UCOServiceImpl implements UCOService {

	private final static Logger log = Logger.getLogger(UCOServiceImpl.class);

	@Override
	public BSInfo parseUCO1(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UCOServiceImpl parseUCO1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Name\\s*(.+)\\s*Branch\\s*Code").trim().replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No\\.\\s*(\\d+)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(UCBA\\w{7})"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "Phone\\s*(\\d*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Phone", 100));
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*of\\s*Account\\s*from\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsUCO_1(pdfText, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in UCOServiceImpl parseUCO1: " + e);
		}

		log.info("Exiting UCOServiceImpl parseUCO1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UCOServiceImpl parseUCO1 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUCO2(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UCOServiceImpl parseUCO2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "\\s{25}(.*):\\s*Primary").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "A/C\\s*NO:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(\\S*)"));
			String dateFormat = "dd-MM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(pdfText, "FOR\\s*THE\\s*PERIOD\\s*OF\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(pdfText, "FOR\\s*THE\\s*PERIOD\\s*OF.*?to\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "CUSTOMER\\s*ADDRESS\\s*:.*\\n([\\s\\S]*?)\\n\\s*Unrecovered", 100));

			List<Transaction> transactions = extractTransactionsUCO_2(pdfText, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in UCOServiceImpl parseUCO2: " + e);
		}

		log.info("Exiting UCOServiceImpl parseUCO2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UCOServiceImpl parseUCO2 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUCO3(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UCOServiceImpl parseUCO3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "7");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "A\\/C\\s*No\\s*:(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Name\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSCCode\\s*:(\\S*)MICR"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "ContactNo\\s*:(.*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "\\s*(.*)A/C").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "ReportDate\\s*:.*\\n([\\s\\S]*?)\\n.*Contact\\s*No", 65, 60));
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "PERIOD\\s*FROM\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bankStatementInfo.setTransactions(extractTransactionsUCO_3(filePath, bankStatementInfo.getAccountNo(), dateFormat));

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in UCOServiceImpl parseUCO3: " + e);
		}
		log.info("Exiting UCOServiceImpl parseUCO3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UCOServiceImpl parseUCO3 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUCO4(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UCOServiceImpl parseUCO4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
//        	(\d{2}-[A-Za-z]{3}-\d{4})\s*(.*?)\s{10,}(\S*)\s*(\S*)
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "account\\s*number\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Name[\\s\\S]*?)\\n\\s*Address", 56).replace("Name", "").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(UCBA\\w{7})"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No.\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "A/c\\s*Type\\s*(.{30})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "E-Mail\\s*ID(.{0,40})").replaceAll("\\s+", "").trim());
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s+.*Address[\\s\\S]*?)\\n\\s*A/c", 56));
			String dateFormat = "dd-MM-yyyy";
			String txnDateFormat = "dd-MMM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Between\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}

			Pattern format1 = Pattern.compile("(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*(\\d+)\\s+(\\d+)");
			Matcher matcher = format1.matcher(pdfText);
			if (matcher.find()) {
				log.info("extractTransactionsUCO_4_2 called");
				bankStatementInfo.setTransactions(extractTransactionsUCO_4_2(pdfText, bankStatementInfo.getAccountNo(), txnDateFormat));
			} else {
				log.info("extractTransactionsUCO_4_1 called");
				bankStatementInfo.setTransactions(extractTransactionsUCO_4_1(pdfText, bankStatementInfo.getAccountNo(), txnDateFormat));
			}

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in UCOServiceImpl parseUCO4: " + e);
		}
		log.info("Exiting UCOServiceImpl parseUCO4: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UCOServiceImpl parseUCO4 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUCO5(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UCOServiceImpl parseUCO5 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "Contact\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*:\\s*(.*)\\s*Drawing").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Name\\s*:\\s*([\\s\\S]*?)\\s*Status").replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Type\\s*:\\s*(.*)"));
			String address1 = CommonUtils.extractMultiLinesField(pdfText, "Credit\\s*Accrued.*\\n([\\s\\S]*?)\\n\\s*Contact", 86).replaceAll("Line\s*1\s*:", " ").trim();
			String address2 = CommonUtils.extractMultiLinesField(pdfText, "Credit\\s*Accrued.*\\n([\\s\\S]*?)\\n\\s*Contact", 86, 80).replaceAll("Line\s*2\s*:", " ").trim();
			bankStatementInfo.setAddress((address1 + " " + address2));

			List<Transaction> listOfTransactions = extractTransactionsUCO_5(pdfText, bankStatementInfo.getAccountNo(), "dd/MM/yyyy");
			bankStatementInfo.setStartDate(listOfTransactions.get(0).getTxnDate());
			bankStatementInfo.setEnDate(listOfTransactions.get(listOfTransactions.size() - 1).getTxnDate());
			bankStatementInfo.setTransactions(listOfTransactions);
		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in UCOServiceImpl parseUCO5: " + e);
		}
		log.info("Exiting UCOServiceImpl parseUCO5: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UCOServiceImpl parseUCO5 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsUCO_1(String text, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
		String[] lines = text.split("\n");

		Pattern transactionPattern = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+([\\d,]*\\.\\d{2})?\\s*([\\d,]*\\.\\d{2})?\\s+([\\d,]*\\.\\d{2})");
		long sNo = 1;
		for (String line : lines) {
			Matcher matcher = transactionPattern.matcher(line);

			if (matcher.find()) {
				Transaction currentTransaction = new Transaction();
				currentTransaction.setsNo(String.valueOf(sNo++));
				currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
				currentTransaction.setDescription(matcher.group(2).replaceAll("\\s+", " ").trim());
				currentTransaction.setAccNo(accountNo);

				String withdrawals = matcher.group(3) != null ? matcher.group(3) : "";
				String balance = matcher.group(5) != null ? matcher.group(5) : "";

				int firstNumberStart = line.indexOf(withdrawals);
				int secondNumberStart = line.indexOf(balance);

				if ((secondNumberStart - firstNumberStart) > 30) {
					currentTransaction.setDebit(withdrawals);
					currentTransaction.setTxnType("DEBIT");
					currentTransaction.setCredit("");
				} else {
					currentTransaction.setCredit(withdrawals);
					currentTransaction.setTxnType("CREDIT");
					currentTransaction.setDebit("");
				}
				currentTransaction.setAmount(withdrawals);
				currentTransaction.setBalance(balance);

				transactions.add(currentTransaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUCO_2(String text, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile("^\\s*(\\d{2}-\\d{2}-\\d{4})\\s+(.{55}).{21}\\s+([\\d\\.,]+)(\\s+)([\\d\\.,]+)", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(text);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription(matcher.group(2).replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(3));
			transaction.setBalance(matcher.group(5));
			if (matcher.group(4).length() > 26) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(transaction.getAmount());
				transaction.setTxnType("CREDIT");
			}
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUCO_3(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		int serialNumCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 6 && !data[0].equals("DATE")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);
					transaction.setBalance(data[5].replace("CR", "").trim());
					transaction.setCredit(data[4]);
					transaction.setDebit(data[3]);
					if (transaction.getDebit().equals("")) {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					} else {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					}
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUCO_4_1(String text, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
//		String pattern1 = "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*(\\S*)(\\s*)(\\S*)\\n([\\s\\S]*?)\\n\\s*\\d{2}-[A-Za-z]{3}-\\d{4}";
		String pattern2 = "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*([\\s\\S]*?)\\s+([\\d\\.,]+)(\\s+)([\\d\\.,]+)";
		Pattern pattern = Pattern.compile(pattern2);
		Matcher matcher = pattern.matcher(text);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription(matcher.group(2).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(3));
			transaction.setBalance(matcher.group(5));
			if (matcher.group(4).length() >= 24) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(transaction.getAmount());
				transaction.setTxnType("CREDIT");
			}
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUCO_4_2(String text, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
		String pattern1 = "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*(\\S*)(\\s*)(\\S*)\\n([\\s\\S]*?)(?=\\s*\\d{2}-[A-Za-z]{3}-\\d{4}|Closing)";
		String pattern2 = "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*([\\s\\S]*?)\\s+([\\d\\.,]+)(\\s+)([\\d\\.,]+)";
		Pattern pattern = Pattern.compile(pattern1);
		Matcher matcher = pattern.matcher(text);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			String amount = matcher.group(2);
			transaction.setAmount(amount);
			String whitespaces = matcher.group(3);
			transaction.setBalance(matcher.group(4));

			transaction.setDescription(matcher.group(5).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());

			if (whitespaces.length() >= 20) { // debit
				transaction.setDebit(amount);
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(amount);
				transaction.setTxnType("CREDIT");
			}
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUCO_5(String text, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{4})\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*([\\d,\\.]+)(\\s+)([\\d,\\.]+)\\s*([\\s\\S]*?)(?=(\\d{2}\\/\\d{2}\\/\\d{4}) | Date)");
		Matcher matcher = pattern.matcher(text);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 25) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setCredit(transaction.getAmount());
				transaction.setDebit("");
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setDescription(matcher.group(6).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}
}