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
public class DLXBServiceImpl implements DLXBService {

	private final static Logger log = Logger.getLogger(DLXBServiceImpl.class);

	@Override
	public BSInfo parseDLXB1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering DLXBServiceImpl parseDLXB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Title\\s*:(.{1,80})").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Account\\s*Branch\\s*:(.{1,80})").replaceAll("\\s+", " "));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*:\\s*(\\S*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Account\\s*Title.*([\\s\\S]*?)\\n\\s*DATE", 110));
			String dateFormat = "dd-MMM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*:\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*:.*?To\\s*(\\S*)"), dateFormat));

			List<Transaction> transactions = extractTransactionsDLXB_1(filePath, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in DLXBServiceImpl parseDLXB1: " + e);
		}
		log.info("Exiting DLXBServiceImpl parseDLXB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for DLXBServiceImpl parseDLXB1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseDLXB2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering DLXBServiceImpl parseDLXB2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "6");
			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)\\n\\s*C\\s*u\\s*s\\s*t\\s*o\\s*m\\s*e\\s*r\\s*I\\s*D").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.{0,25})"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(.{0,15})").replaceAll("\\s*", ""));

			String address = CommonUtils.extractMultiLinesField(text, "P\\s*A\\s*N.*\\n([\\s\\S]*)?\\n\\s*M\\s*o\\s*b\\s*i\\s*l\\s*e", 40);
			String pan = CommonUtils.extractField(text, "PAN\\s*:\\s*(\\S+)");
			bankStatementInfo.setPan(pan);

			if (pan != null && !pan.isEmpty() && !pan.contains("***")) {
				address = address.replaceFirst(Pattern.quote(pan), "").replaceAll("\\s+", " ").trim();
			}

			bankStatementInfo.setAddress(address);

			String dateFormat = "dd/MM/yyyy";

			List<Transaction> transactions = extractTransactionsDLXB_2(text, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setStartDate(transactions.get(0).getTxnDate());
			bankStatementInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in DLXBServiceImpl parseDLXB2: " + e);
		}
		log.info("Exiting DLXBServiceImpl parseDLXB2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for DLXBServiceImpl parseDLXB2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsDLXB_(String text, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();

//		Matcher matcherRemove = Pattern
//				.compile("^\\s*\\*Closing\\s*balance\\s*in[\\s\\S]*?\\(DD/MM/YYYY\\s*\\)$", Pattern.MULTILINE)
//				.matcher(text);
//		text = matcherRemove.replaceAll("");

		Pattern transactionPattern = Pattern
				.compile("^(?!\\s*\\d{2}/\\d{2}/\\d{4})\\s*(.*?)\\s{10,}([\\d,.]+)(Cr|Dr)\\s*(\\d{2}/\\d{2}/\\d{4})([\\s\\S]*?)(\\d{2}/\\d{2}/\\d{4}).*?Balance:\\s*([\\d,.]+)", Pattern.MULTILINE);

		Matcher matcher = transactionPattern.matcher(text);

		long serialNoCount = 1;
		while (matcher.find()) {
			System.out.println(matcher.group());
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(4), "dd/MM/yyyy"));
			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(6), "dd/MM/yyyy"));

			String description = matcher.group(1) + " " + matcher.group(5);
			description = description.replaceAll("\\s+", " ");
			if (matcher.group(3).equalsIgnoreCase("Dr")) {
				transaction.setTxnType("DEBIT");
				transaction.setDebit(matcher.group(2));
				transaction.setAmount(matcher.group(2));
			} else {
				transaction.setTxnType("CREDIT");
				transaction.setCredit(matcher.group(2));
				transaction.setAmount(matcher.group(2));

			}
			transaction.setBalance(matcher.group(6));

			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsDLXB_1(String filePath, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data[0].equals("Opening Balance"))
					break;
				if (data.length == 7 && !data[0].equals("DATE")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);
					if (data[4].equals("0.00")) { // Credit
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
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsDLXB_2(String pdfText, String accountNo, String dateFormat) {

//		(\d{2}\/\d{2}\/\d{4})(.*)?(\d{2}\/\d{2}\/\d{4}).*?Balance\s*:\s*(-?\d*,?\d*,?\d+\.\d+)
//		(.*)?(-?\d*,?\d*,?\d+\.\d+)([C\D]r)
//		(\d{2}\/\d{2}\/\d{4})\s+(\d{2}\/\d{2}\/\d{4})\s+(\S+)
//		(.*)?Balance\s*:\s*(\S+)
//		\s*\*?Closing\s*balance[\s\S]*?\(D\s*D\s*\/M\s*M\s*\/Y\s*Y\s*Y\s*Y\s*.*
//		DebitCountTotalDebitsCreditsCount
//		\(DD\/MM\/YYYY\)\(DD\/MM\/YYYY\)

		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("\\s*\\*?Closing\\s*balance[\\s\\S]*?\\(D\\s*D\\s*\\/M\\s*M\\s*\\/Y\\s*Y\\s*Y\\s*Y\\s*.*", "");
		try {

			Pattern txnPattern1 = Pattern.compile("(.*)\\s{10}(\\S+)([CD]r)");
			Pattern txnPattern2 = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{4})(.*)?(\\d{2}\\/\\d{2}\\/\\d{4}).*?Balance\\s*:\\s*(\\S+)");
			Pattern txnPattern3 = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{4})\\s+(\\d{2}\\/\\d{2}\\/\\d{4})\\s+(\\S+)");
			Pattern txnPattern4 = Pattern.compile("(.*)?Balance\\s*:\\s*(\\S+)");

			String txnStart = "(DD/MM/YYYY)(DD/MM/YYYY)";
			Pattern pdfEnd = Pattern.compile("Debit\\s*Count\\s*Total\\s*Debits\\s*Credit\\s*Count");
			boolean txnstatus = false;

			String[] lines = pdfText.split("\\n");
			int serialNoCount = 1;
			Transaction transaction = new Transaction();
			for (String eachLine : lines) {
//				System.out.println(eachLine);
//				eachLine = eachLine.trim();
				String linewithoutSpace = eachLine.replaceAll("\\s", "");
				Matcher pdfEndMatcher = pdfEnd.matcher(eachLine);
				if (linewithoutSpace.contains(txnStart)) {
					txnstatus = true;
					continue;
				}
				if (pdfEndMatcher.find()) {
//					System.out.println(transaction.toString());
					if (transaction.getAmount() != null) {
						transactions.add(transaction);
					}
					break;
				}
				if (txnstatus) {

					Matcher txnMatcher1 = txnPattern1.matcher(eachLine);
					Matcher txnMatcher2 = txnPattern2.matcher(eachLine);
					Matcher txnMatcher3 = txnPattern3.matcher(eachLine);
					Matcher txnMatcher4 = txnPattern4.matcher(eachLine);

					if (txnMatcher1.find()) {
						if (transaction.getAmount() != null) {
							transaction.setDescription(transaction.getDescription().replaceAll("\\s+", " ").trim());
							transactions.add(transaction);
							transaction = new Transaction();
						}
						transaction.setsNo(Integer.toString(serialNoCount++));
						transaction.setAccNo(accountNo);
						transaction.setDescription(txnMatcher1.group(1));
						String amount = CommonUtils.cleanAmountString(txnMatcher1.group(2));
						transaction.setAmount(amount);
						String type = txnMatcher1.group(3);
						if (type.equalsIgnoreCase("Dr")) {
							transaction.setDebit(amount);
							transaction.setTxnType("DEBIT");
							transaction.setCredit("");
						} else if (type.equalsIgnoreCase("Cr")) {
							transaction.setCredit(amount);
							transaction.setTxnType("CREDIT");
							transaction.setDebit("");
						}
					} else if (txnMatcher2.find()) {
//						System.out.println(eachLine);
						transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher2.group(1), dateFormat));
						transaction.setValueDate(CommonUtils.dateFormatter(txnMatcher2.group(3), dateFormat));
						String desc = (transaction.getDescription() != null && !transaction.getDescription().equals("")) ? transaction.getDescription() : "";
						desc += txnMatcher2.group(2);
						transaction.setDescription(desc);
						transaction.setBalance(CommonUtils.cleanAmountString(txnMatcher2.group(4)));
					} else if (txnMatcher3.find()) {
						transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher3.group(1), dateFormat));
						transaction.setValueDate(CommonUtils.dateFormatter(txnMatcher3.group(2), dateFormat));
						transaction.setTxnId(txnMatcher3.group(3));
					} else if (txnMatcher4.find()) {
						String desc = (transaction.getDescription() != null && !transaction.getDescription().equals("")) ? transaction.getDescription() : "";
						desc += txnMatcher4.group(1);
						transaction.setDescription(desc);
						transaction.setTxnId(txnMatcher4.group(2));
					} else {
						String desc = (transaction.getDescription() != null && !transaction.getDescription().equals("")) ? transaction.getDescription() : "";
						desc += eachLine;
						desc = desc.replaceAll("\\s+", " ").trim();
						transaction.setDescription(desc);
					}
				}
			}
		} catch (Exception e) {
			log.error("Error in extractTransactionsDLXB_2", e);
		}
		return transactions;
	}

//	private List<Transaction> extractTransactionsDLXB_2(String pdfText, String accountNo, String dateFormat) {
//		List<Transaction> transactions = new ArrayList<>();
//
//		Pattern pattern = Pattern.compile(
//				"\\s*(.*?)\\s*([\\d,]+\\.\\d{2})(\\wr)\\n\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s+(.*?)\\s+(\\d{2}\\/\\d{2}\\/\\d{4})[\\s\\S]*?\\s+Balance:\\s*([\\d,]+\\.\\d{2})([\\s\\S]*?)(?=(\\s*\\*Closingbalance)|(\\n.*?[\\d,]+\\.\\d{2}\\wr))");
//		// 1 is desc 2 is amount, 3 is Dr/Cr 4 trx date 5 desc2, 6 value date, 7
//		// balance, 8 desc 3
//		Matcher matcher = pattern.matcher(pdfText);
//		int serialNoCount = 1;
//
//		while (matcher.find()) {
//			Transaction transaction = new Transaction();
//			transaction.setsNo(String.valueOf(serialNoCount++));
//			String description = matcher.group(1) + " " + matcher.group(5) + " " + matcher.group(8);
//			transaction.setDescription(description.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
//			transaction.setAmount(matcher.group(2));
//			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(4), dateFormat));
//			if (matcher.group(3).equalsIgnoreCase("Dr")) {
//				transaction.setDebit(transaction.getAmount());
//				transaction.setCredit("");
//				transaction.setTxnType("DEBIT");
//			} else {
//				transaction.setDebit("");
//				transaction.setCredit(transaction.getAmount());
//				transaction.setTxnType("CREDIT");
//			}
//			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(6), dateFormat));
//			transaction.setBalance(matcher.group(7));
//			transaction.setAccNo(accountNo);
//			System.out.println(transaction);
//			transactions.add(transaction);
//		}
//		return transactions;
//	}
}
