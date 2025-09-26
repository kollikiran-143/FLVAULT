package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class AXISServiceImpl implements AXISService {

	private final static Logger log = Logger.getLogger(AXISServiceImpl.class);

	@Override
	public BSInfo parseAXIS1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS1 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			String[] arraytoStrings = CommonUtils.extractMultiGroupArray(text, "Statement.*Account\\s*No:\\s*(.+?)\\s+.*?From:\\s*(.*)To:\\s*(.*)\\)");
			String dateFormat = "dd-MM-yyyy";
			if (arraytoStrings != null) {
				bankstatementInfo.setAccountNo(arraytoStrings[0]);
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(arraytoStrings[1], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(arraytoStrings[2], dateFormat));
			}
			bankstatementInfo.setName(CommonUtils.extractField(text, "CUSTOMER\\s*NAME\\s*(.+)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiGroupConcatenate(text, "CUSTOMER\\s{10}\\s*(.*)\\n\\s*ADDRESS\\s*(.*)"));
			bankstatementInfo.setTransactions(extractTransactionsAXIS_1(filepath, bankstatementInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS1: " + e);
		}

		log.info("Exiting AXISServiceImpl parseAXIS1: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS2 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS2 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setName(CommonUtils.extractField(text, "(.*)\\s*(?=Joint)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Joint.*-\\s*\\n([\\s\\S]*?)\\n\\s*Currency", 85));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*:(.*)"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee.*:(.*)"));
			String[] basicDetail = CommonUtils.extractMultiGroupArray(text, "Account\\s*No\\s*:\\s*(\\d*)\\s*for\\s*the\\s*period.*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");

			String dateFormat = "dd-MM-yyyy";
			if (basicDetail != null) {
				bankstatementInfo.setAccountNo(basicDetail[0]);
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(basicDetail[1], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(basicDetail[2].replaceAll("\\)", ""), dateFormat));
			}
			List<Transaction> transactions = extractTransactionsAXIS_2(filepath, bankstatementInfo.getAccountNo());
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS2: " + e);
		}
		log.info("Exiting AXISServiceImpl parseAXIS2: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS2 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS3(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS3 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setName(CommonUtils.extractField(text, "(.*)\\s*(?=Joint)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Joint.*-\\s*\\n([\\s\\S]*?)\\n\\s*Registered\s*Mobile", 85));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*:\\s*(UTIB\\w{7})"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee.*:(.*)").trim());
			bankstatementInfo.setPan(CommonUtils.extractField(text, "PAN\\s*:\\s*(.*)"));
			bankstatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*:(\\+?[X\\d]*)"));
			bankstatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*ID\\s*:\\s*(.*@\\w*\\.\\w*)"));
			bankstatementInfo.setAccountType(CommonUtils.extractField(text, "Scheme\\s*:(.*)CKYC").replaceAll("\\s+", " ").trim());
			String[] accNoPeriod = CommonUtils.extractMultiGroupArray(text, "Account\\s*No\\s*:\\s*(\\d*)\\s*.*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			String dateFormat = "dd-MM-yyyy";
			if (accNoPeriod != null) {
				bankstatementInfo.setAccountNo(accNoPeriod[0]);
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2].replaceAll("\\)", ""), dateFormat));
			}
			bankstatementInfo.setTransactions(extractTransactionsAXIS_3(filepath, bankstatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS3: " + e);
		}
		log.info("Exiting AXISServiceImpl parseAXIS3: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS3 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS4 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\.\\s*([X\\d]*)"));
			bankstatementInfo.setName(CommonUtils.extractField(text, "(.*)\\n?\\s*Customer\\s*ID\\s*:").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "\\n(\\s*Registered\\s*Address[\\s\\S]*?)\\n\\s*Profile\\s*Complete", 75).replaceAll("\\s*Registered\\s*", ""));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*:\\s*(UTIB\\w{7})"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setPan(CommonUtils.extractField(text, "PAN\\s*:\\s*(\\w{10})"));
			bankstatementInfo.setPhone1(CommonUtils.extractField(text, "Registered\\s*Mobile\\s*No\\s*:\\s*(.*)"));
			bankstatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*ID\\s*:\\s*(\\S*)"));
			bankstatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:(.{50})").replaceAll("\\s+", " ").trim());
			String[] period = CommonUtils.extractMultiGroupArray(text, "Summary\\s*of\\s*accounts.*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsAXIS_4(filepath, bankstatementInfo.getAccountNo());
			if (transactions == null || transactions.isEmpty()) {
				System.out.println("transaction is empty");
			}
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS4: " + e);
		}
		log.info("Exiting AXISServiceImpl parseAXIS4: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS4 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS5(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS5 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankstatementInfo.setName(CommonUtils.extractField(text, "(.{30,})SCHEME\\s*CODE").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "SCHEME\\s*CODE.*\\n([\\s\\S]*?)\\n\\s*Tran\\s*Date", 80));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*(UTIB\\w{7})"));
			String[] accNoperiod = CommonUtils.extractMultiGroupArray(text, "STATEMENT\\s*BETWEEN\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*FOR\\s*A\\/C\\s*:\\s*(\\S*)");
			String dateFormat = "dd/MM/yyyy";
			if (accNoperiod != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(accNoperiod[0], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(accNoperiod[1], dateFormat));
				bankstatementInfo.setAccountNo(accNoperiod[2]);
			}
			List<Transaction> transactions = extractTransactionsAXIS_5(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS5: " + e);
		}
		log.info("Exiting AXISServiceImpl parseAXIS5: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS5 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS6(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS6 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setName(CommonUtils.extractField(text, "\\s*(.*?)Customer\\s*ID").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setPan(CommonUtils.extractField(text, "PAN\\s*Number\\s*:\\s*(\\S*)"));
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*(\\S*)"));
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\s*ID.*\\n([\\s\\S]*?)(?=(\\s*Statement))", 100));

			String dateFormat = "dd-MM-yyyy";

			bankstatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "period.*?from:\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankstatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "period.*?to(\\d{2}-\\d{2}-\\d{4})"), dateFormat));

			List<Transaction> transactions = extractTransactionsAXIS_6(filepath, bankstatementInfo.getAccountNo());
			if (transactions == null || transactions.isEmpty()) {
				System.out.println("transaction is empty");
			}
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS6: " + e);
		}

		log.info("Exiting AXISServiceImpl parseAXIS6: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS6 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS7(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS7 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankstatementInfo.setName(CommonUtils.extractField(text, "(.{30,})SCHEME\\s*CODE").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "SCHEME\\s*CODE.*\\n([\\s\\S]*?)\\n\\s*Tran\\s*Date", 80));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*(UTIB\\w{7})"));
			String[] accNoperiod = CommonUtils.extractMultiGroupArray(text, "STATEMENT\\s*BETWEEN\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*FOR\\s*A\\/C\\s*:\\s*(\\S*)");
			String dateFormat = "dd/MM/yyyy";
			if (accNoperiod != null) {
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(accNoperiod[0], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(accNoperiod[1], dateFormat));
				bankstatementInfo.setAccountNo(accNoperiod[2]);
			}
			List<Transaction> transactions = extractTransactionsAXIS_7(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS7: " + e);
		}

		log.info("Exiting AXISServiceImpl parseAXIS7: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS7 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseAXIS8(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering AXISServiceImpl parseAXIS8 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankstatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Statement.*\\n([\\s\\S]*?)\\n\\s*Joint\\s*Holder").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractField(text, "Joint\\s*Holder\\s*:-([\\s\\S]*?)Scheme").replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*(UTIB\\w{7})"));
			String[] accNoperiod = CommonUtils.extractMultiGroupArray(text, "Account\\s*No\\s*:\\s*(\\S+).*period.*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			String dateFormat = "dd-MM-yyyy";
			if (accNoperiod != null) {
				bankstatementInfo.setAccountNo(accNoperiod[0]);
				bankstatementInfo.setStartDate(CommonUtils.dateFormatter(accNoperiod[1], dateFormat));
				bankstatementInfo.setEnDate(CommonUtils.dateFormatter(accNoperiod[2], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsAXIS_8(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in AXISServiceImpl parseAXIS8: " + e);
		}

		log.info("Exiting AXISServiceImpl parseAXIS8: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for AXISServiceImpl parseAXIS8 is: " + timeTaken);
		return bankstatementInfo;
	}

	private List<Transaction> extractTransactionsAXIS_1(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty() || row.length <= 4) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length >= 6 && !data[0].equals("Transaction Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setAccNo(accountNo);
					String dateFormat = "dd-MM-yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[2]);

					if (data[3] != null && !data[3].equalsIgnoreCase("")) {
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
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_2(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		Set<String> transactionIdentifiers = new HashSet<>(); // To store unique transaction identifiers

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length >= 8 && !data[0].equals("Tran Date") && !data[2].equals("OPENING BALANCE")) {

					String transactionIdentifier = data[0] + data[1] + data[2] + data[4] + data[5] + data[6];
					if (!transactionIdentifiers.contains(transactionIdentifier)) {
						Transaction transaction = new Transaction();
						transaction.setsNo(Integer.toString(serialNoCount++));
						String dateFormat = "dd-MM-yyyy";
						transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
						transaction.setValueDate(data[1]);
						transaction.setDescription(data[2].replaceAll("\\s+", " ").trim());
						transaction.setAmount(data[4]);

						if (data[5].equals("DR")) {
							transaction.setDebit(data[4]);
							transaction.setTxnType("DEBIT");
						} else {
							transaction.setCredit(data[4]);
							transaction.setTxnType("CREDIT");
						}
						transaction.setBalance(data[6]);
						transaction.setAccNo(accountNo);
						transactions.add(transaction);
						transactionIdentifiers.add(transactionIdentifier);
					}
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_3(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		Set<String> transactionIdentifiers = new HashSet<>(); // To store unique transaction identifiers

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length == 7 && !data[0].equals("Tran Date") && !data[2].equals("OPENING BALANCE")) {
					String transactionIdentifier = data[0] + data[2] + (data[3].isEmpty() ? data[4] : data[3]);
					if (!transactionIdentifiers.contains(transactionIdentifier)) {
						Transaction transaction = new Transaction();
						transaction.setsNo(Integer.toString(serialNoCount++));
						String dateFormat = "dd-MM-yyyy";
						transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
						transaction.setDescription(data[2].replaceAll("\\s+", " ").trim());

						if (data[3] != null && !data[3].equalsIgnoreCase("")) {
							transaction.setDebit(data[3]);
							transaction.setTxnType("DEBIT");
							transaction.setCredit("");
							transaction.setAmount(data[3]);
						} else {
							transaction.setCredit(data[4]);
							transaction.setTxnType("CREDIT");
							transaction.setDebit("");
							transaction.setAmount(data[4]);
						}
						transaction.setBalance(data[5]);
						transaction.setAccNo(accountNo);
						transactions.add(transaction);
						transactionIdentifiers.add(transactionIdentifier);
					}
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_4(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		Pattern datePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		Set<String> transactionIdentifiers = new HashSet<>(); // To store unique transaction identifiers

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 6) {
					Matcher dateMatcher = datePattern.matcher(data[0]);
					String transactionIdentifier = data[0] + data[1] + (data[3].isEmpty() ? data[4] : data[3]);
					if (!transactionIdentifiers.contains(transactionIdentifier) && dateMatcher.find()) {
						Transaction transaction = new Transaction();
						transaction.setsNo(Integer.toString(serialNoCount++));
						String dateFormat = "dd-MM-yyyy";
						transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
						transaction.setDescription(data[1].replaceAll("\\s+", " ").trim());

						if (data[3] != null && !data[3].equalsIgnoreCase("")) {
							transaction.setDebit(data[3]);
							transaction.setTxnType("DEBIT");
							transaction.setCredit("");
							transaction.setAmount(data[3]);
						} else {
							transaction.setCredit(data[4]);
							transaction.setTxnType("CREDIT");
							transaction.setDebit("");
							transaction.setAmount(data[4]);
						}
						transaction.setBalance(data[5]);
						transaction.setAccNo(accountNo);
						transactions.add(transaction);
						transactionIdentifiers.add(transactionIdentifier);
					}
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_5(String fileName, String accountNo, String dateFormat) throws IOException {
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
				if (data.length == 8 && !data[0].equals("Tran Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2].replaceAll("\\s+", " ").trim());
					String amount = data[4];
					transaction.setAmount(amount);
					String txnType = data[5];
					if (txnType.equalsIgnoreCase("Dr")) {
						transaction.setDebit(amount);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setCredit(amount);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setBalance(data[6]);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_6(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty() || row.length <= 4) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 8 && !data[3].equals("Particulars")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setAccNo(accountNo);
					String dateFormat = "dd-MM-yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[3]);

					if (data[4] == null || data[4].equals("") || data[4].equals("-")) { // credit
						transaction.setAmount(data[5]);
						transaction.setCredit(transaction.getAmount());
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					} else {
						transaction.setAmount(data[4]);
						transaction.setCredit("");
						transaction.setDebit(transaction.getAmount());
						transaction.setTxnType("DEBIT");
					}
					transaction.setBalance(data[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_7(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty() || row.length <= 4) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 8 && !data[0].equals("Tran Date") && !data[2].contains("OPENING")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setAccNo(accountNo);

					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);

					if (data[4] == null || data[4].equals("") || data[4].equals("-")) { // credit
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					} else {
						transaction.setAmount(data[4]);
						transaction.setCredit("");
						transaction.setDebit(data[4]);
						transaction.setTxnType("DEBIT");
					}
					transaction.setBalance(data[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsAXIS_8(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		Pattern datePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");

				if (line.trim().isEmpty() || row.length <= 4) {
					continue;
				}
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[1]);
				if (data.length == 9 && dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setAccNo(accountNo);

					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[2], dateFormat));
					transaction.setDescription(data[3]);
					transaction.setAmount(data[5]);

					if (!data[6].equals("") && data[6].equalsIgnoreCase("CR")) { // credit
						transaction.setCredit(transaction.getAmount());
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					} else {
						transaction.setCredit("");
						transaction.setDebit(transaction.getAmount());
						transaction.setTxnType("DEBIT");
					}
					transaction.setBalance(data[7]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}