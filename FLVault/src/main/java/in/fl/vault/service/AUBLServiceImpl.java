package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class AUBLServiceImpl implements AUBLService {

	private final static Logger log = Logger.getLogger(AUBLServiceImpl.class);

	@Override
	public BSInfo parseAUBL1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AUBLServiceImpl parseAUBL1 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			bankstatementInfo.setName(CommonUtils.extractField(text, "HOLDER\\s*NAME\\s*:\\s*(.*)BRANCH").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "BRANCH\\s*CODE.*([\\s\\S]*?)(?=CUSTOMER)", 125).replaceAll("ADDRESS\s*:\s*", ""));
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "ACCOUNT\\s*NO\\.\\s*:\\s*(\\S*)"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));

			String dateFormat = "dd-MMM-yyyy";
			bankstatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "STATEMENT\\s*PERIOD\\s*:\\s*(\\d{2}-\\w{3}-\\d{4})"), dateFormat));
			bankstatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "STATEMENT\\s*PERIOD\\s*:\\s*\\S*\\s*-\\s*(\\d{2}-\\w{3}-\\d{4})"), dateFormat));
			bankstatementInfo.setTransactions(extracTransactionsAUBL_1(text, bankstatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AUBLServiceImpl parseAUBL1: " + e);
		}

		log.info("Exiting AUBLServiceImpl parseAUBL1: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AUBLServiceImpl parseAUBL1 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAUBL2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AUBLServiceImpl parseAUBL1 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		String dateFormat = "ddMMMyyyy";
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			bankstatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Nam\\s*e\\s*:\\s*([\\s\\S]{0,55})").replaceAll("\\s+", " ").trim());
			String dates[] = CommonUtils.extractField(text, "StatementFrom\\s*(.*)").replaceAll("\\s*", "").split("To");
			if (dates.length == 2) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(dates[0], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(dates[1], dateFormat));
			}
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(Address[\\s\\S]*?)(?=\\s*AccountNum)", 19, 50).replaceAll(":", "").replaceAll("\\s+", " "));
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "AccountNum\\s*ber\\s*-\\s*(\\d*)"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSCCode-(.{25})").trim());

			bankstatementInfo.setTransactions(extracTransactionsAUBL_2(text, bankstatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AUBLServiceImpl parseAUBL1: " + e);
		}

		log.info("Exiting AUBLServiceImpl parseAUBL2: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AUBLServiceImpl parseAUBL1 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAUBL3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AUBLServiceImpl parseAUBL3 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		String dateFormat = "ddMMMyyyy";
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(AUBL\\w{7})").trim());
			bankstatementInfo.setName(CommonUtils.extractField(text, "ACCOUNT.*\\n\\s*Name\\s*:\\s*(.{40})").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(\\s*Address[\\s\\S]*?)\\n\\s*Statement\\s*Date", 75).replaceAll("\\s+", " ").trim());
			bankstatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			String period[] = CommonUtils.extractMultiGroupArray(text, "Statement\\s*Period\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			if (period != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), dateFormat));
			}

			bankstatementInfo.setTransactions(extracTransactionsAUBL_3(filepath, bankstatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AUBLServiceImpl parseAUBL3: " + e);
		}

		log.info("Exiting AUBLServiceImpl parseAUBL3: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AUBLServiceImpl parseAUBL3 is: " + timeTaken);
		return bankstatementInfo;
	}

	private List<Transaction> extracTransactionsAUBL_1(String pdfText, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*?Opening\\s*Balance.*\\n", "");

		Pattern pattern = Pattern.compile("(\\d{2}-\\w{3}-\\d{4})\\s*(.{70})\\s*([\\d,\\.]+)(\\s+)([\\d,\\.]+)([\\s\\S]*?)(?=((\\d{2}-\\w{3}-\\d{4})|Account))");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription((matcher.group(2) + " " + matcher.group(6)).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());

			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 30) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setCredit(transaction.getAmount());
				transaction.setDebit("");
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extracTransactionsAUBL_2(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*?Opening\\s*Balance.*\\n", "").replaceAll("(?m)^\\s*\\d*\\n*\\s*Pleasereview\\s*theAcco\\s*untBalance[\\s\\S]*?Balance\\R?", "");

//		FileUtils.writeStringToFile(new File("/home/deepak/Documents/test.txt"), pdfText);

		Pattern pattern = Pattern.compile("^(\\s*\\d{2}\\w{3}\\d{4}[\\s\\S]{0,45}.*?\\d{2}\\w{3}\\d{4}.*\\n[\\s\\S]*?)(?=^\\s*\\d{2}\\D{3}\\d{4})", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		String previousLine = "";
		while (matcher.find()) {
			String txnDate = "";
			String description = "";
			String valueDate = "";
			String balance = "";
			String credit = "";
			String debit = "";

			String lines[] = matcher.group(1).split("\\n");
			description = previousLine;
			if (lines.length > 0 && lines[0].trim().isEmpty()) {
				lines = Arrays.copyOfRange(lines, 1, lines.length);
			}

			for (int j = 0; j < lines.length; j++) {
				String descLine = lines[j].length() > 62 ? lines[j].substring(16, 62) : lines[j];

				if (j == 0) {

					txnDate = lines[j].substring(0, 17).trim();

					if (lines[j].length() > 62 && lines[j].length() < 80) {
						valueDate = lines[j].substring(63).trim();
					}
				}
				if (j < lines.length - 1) {
					description += descLine;

					if (lines[j].length() > 152) {
						if (!lines[j].substring(63, 77).trim().isEmpty()) {
							valueDate = lines[j].substring(63, 77).trim();
						}
						debit = lines[j].substring(111, 131).trim();
						credit = lines[j].substring(131, 152).trim();
						balance = lines[j].substring(152).trim();
					}
				} else {
					if (lines[0].substring(17, 60).trim().isEmpty() && j < 2) {
						description += descLine;
						previousLine = "";
					} else {
						previousLine = descLine;
					}
				}
			}
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			debit = debit.replaceFirst("\\S*?\\s+", "");
			if (credit.equalsIgnoreCase("-")) {
				transaction.setTxnType("DEBIT");
				credit = "";
				transaction.setAmount(debit);
			} else {
				debit = "";
				transaction.setTxnType("CREDIT");
				transaction.setAmount(credit);
			}
			txnDate = CommonUtils.dateFormatter(txnDate, dateFormat);
			valueDate = CommonUtils.dateFormatter(valueDate, dateFormat);
			description = description.replaceAll("\\s+", " ").trim();
			transaction.setDescription(description);
			transaction.setTxnDate(txnDate);
			transaction.setValueDate(valueDate);
			transaction.setCredit(credit);
			transaction.setDebit(debit);
			transaction.setBalance(balance);
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extracTransactionsAUBL_3(String fileName, String accountNo, String dateFormat) throws IOException {
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

				if (data.length >= 7 && !data[0].equalsIgnoreCase("Transaction Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));

					transaction.setTxnDate(CommonUtils.dateFormatter(data[0].replaceAll("\\s*", ""), dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1].replaceAll("\\s*", ""), dateFormat));
					transaction.setDescription(data[2]);
					transaction.setTxnId(data[3]);

					if (!data[4].isEmpty() && !data[4].equals("-")) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setDebit("");
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
}
