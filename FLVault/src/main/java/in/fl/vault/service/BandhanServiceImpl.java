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
public class BandhanServiceImpl implements BandhanService {

	private final static Logger log = Logger.getLogger(BandhanServiceImpl.class);

	@Override
	public BSInfo parseBandhan1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BandhanServiceImpl parseBandhan1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*(\\d*)");
			if (accountNo != null && !accountNo.equalsIgnoreCase("")) {
				bankStatementInfo.setAccountNo(accountNo);
			} else {
				bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*(\\d*)"));
			}

			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*(BDBL\\w{7})").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Statement\\n(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Details\\s*(.*)Branch").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Current.*\\n([\\s\\S]*?)\\n\\s*Customer\\s*Account", 70));

			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nomination\\s*Registered\\s*(.*)\\n").replaceAll("\\s+", " ").trim());
			List<Transaction> transactions = extractTransactionsBANDHAN_1(filePath, accountNo);
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*period\\s*From\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			String dateFormat = "dd MMM yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s+", " "), dateFormat));
			}
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in BandhanServiceImpl parseBandhan1: " + e);
		}

		log.info("Exiting BandhanServiceImpl parseBandhan1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BandhanServiceImpl parseBandhan1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseBandhan2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BandhanServiceImpl parseBandhan2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(BDBL\\w{7})"));
			bankStatementInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "(\\s*Account\\s*Title\\s*:[\\s\\S]*?)\\n\\s*Joint\\s*Holder", 100).replaceAll("Account\\s*Title\\s*:", ""));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Account\\s*Branch\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*From\\s*Date", 100));

			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*(.*)Branch\\s*ID").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*Registered\\s*:\\s*(.*)IFSC").replaceAll("\\s+", " "));
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "From\\s*Date.*(\\d{2}-[A-Z]{3}-\\d{4}).*(\\d{2}-[A-Z]{3}-\\d{4})");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s+", " "), dateFormat));
			}
			List<Transaction> transactions = extractTransactionsBANDHAN_2(filePath, accountNo, dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in BandhanServiceImpl parseBandhan2: " + e);
		}

		log.info("Exiting BandhanServiceImpl parseBandhan2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BandhanServiceImpl parseBandhan2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseBandhan3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BandhanServiceImpl parseBandhan3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*N\\s*umber\\s*:\\s*(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(BDBL\\w{7})"));
			bankStatementInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "(\\s*Customer\\s*Name\\s*:[\\s\\S]*?)\\n\\s*Customer\\s*Address", 75).replaceAll("Customer\\s*Name\\s*:\\s*", ""));
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*T\\s*ype\\s*:\\s*(.*)MICR").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*of\\s*Ow\\s*nership\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "(\\s*Customer\\s*Address[\\s\\S]*?)\\n\\s*Customer\\s*Number", 75).replaceAll("Customer\\s*", ""));
			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "N\\s*ominee\\s*Registration\\s*:\\s*(.*)Branch").replaceAll("\\s+", " "));

			List<Transaction> transactions = extractTransactionsBANDHAN_3(pdfText, accountNo);
			bankStatementInfo.setStartDate(transactions.get(0).getTxnDate());
			bankStatementInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in BandhanServiceImpl parseBandhan3: " + e);
		}

		log.info("Exiting BandhanServiceImpl parseBandhan3: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BandhanServiceImpl parseBandhan3 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsBANDHAN_1(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
//		String dateFormat = "MMMM dd, yyyy";
//		Pattern datePattern = Pattern.compile("\\w{4,}\\s\\d{2},\\s\\d{4}");
		String dateFormat = "MMMMdd,yyyy";
		Pattern datePattern = Pattern.compile("\\w{4,}\\s*\\d{2},\\s\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}

				String[] lineArr = line.split("\\|");
				if (lineArr.length != 6)
					continue;
				Matcher dateMatcher = datePattern.matcher(lineArr[0]);
				if (dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(lineArr[0].replaceAll("\\s+", ""), dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(lineArr[1].replaceAll("\\s+", ""), dateFormat));
					String desc = lineArr[2];
					String[] lineArr3Split = lineArr[3].replaceAll("INR", "").trim().split("\\s");

					for (int i = 0; i < lineArr3Split.length; i++) {
						if (i == 0) {
							transaction.setAmount(lineArr3Split[0]);
						} else if (i > 0) {
							desc += lineArr3Split[i];
						}
					}
					transaction.setDescription(desc);
					if (lineArr[4].equalsIgnoreCase("Cr")) {
						transaction.setCredit(transaction.getAmount());
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					} else if (lineArr[4].equalsIgnoreCase("Dr")) {
						transaction.setDebit(transaction.getAmount());
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					}
					transaction.setBalance(lineArr[5].substring(3).trim());
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsBANDHAN_2(String filePath, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		Pattern datePattern = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] lineArr = line.split("\\|");

				Matcher dateMatcher = datePattern.matcher(lineArr[0].replaceAll("\\s*", ""));
				if (dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(lineArr[0].replaceAll("\\s*", ""), dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(lineArr[1].replaceAll("\\s*", ""), dateFormat));
					transaction.setTxnId(lineArr[2]);
					transaction.setDescription(lineArr[3]);
					String debit = lineArr[4];
					String credit = lineArr[5];

					if (debit != null && !debit.equalsIgnoreCase("") && !debit.equalsIgnoreCase("0.00")) {
						transaction.setDebit(debit);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
						transaction.setAmount(debit);
					} else {
						transaction.setCredit(credit);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
						transaction.setAmount(credit);
					}
					transaction.setBalance(lineArr[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsBANDHAN_3(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("-This\\s*is\\s*computer\\s*generated\\s*statement[\\s\\S]*?\\s*W\\s*ebsite:www.bandhanbank.com", "").replaceAll("page[\\s\\S]*?\\s*Statement\\s*of\\s*Account.*",
				"");

		String dateFormat = "dd/MM/yyyy";
		String[] lines = pdfText.split("\n");
		Pattern transactionPattern = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{4})\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).{20}\\s*([\\s\\S]*?)\\s{10,}(\\S*)\\s*(\\S*)\\s*(\\S*)");
		Pattern pageEnd = Pattern.compile("Statement\\s*Period\\s*Opening\\s*No\\s*of\\s*Debit");
		Transaction transaction = null;
		long serialNoCount = 1;
		for (String line : lines) {
			Matcher pageEndMatcher = pageEnd.matcher(line.trim());

			if (!pageEndMatcher.find()) {
				Matcher matcher = transactionPattern.matcher(line.trim());

				if (matcher.find()) {
					transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
					transaction.setDescription(matcher.group(3).replaceAll("\\s+", " "));

					String debit = matcher.group(4);
					String credit = matcher.group(5);
					String balance = matcher.group(6);
					if (debit != null && !debit.equalsIgnoreCase("") && !debit.equalsIgnoreCase("0.00")) {
						transaction.setDebit(debit);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
						transaction.setAmount(debit);
					} else {
						transaction.setCredit(credit);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
						transaction.setAmount(credit);
					}
					transaction.setBalance(balance);
					transactions.add(transaction);
				} else if (transaction != null) {
					transaction.setDescription((transaction.getDescription() + " " + line.replaceAll("\\s+", " ")).trim());
				}
			} else {
				transaction = null;
			}
		}
		return transactions;
	}

}