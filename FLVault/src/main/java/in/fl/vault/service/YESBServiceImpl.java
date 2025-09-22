package in.fl.vault.service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
public class YESBServiceImpl implements YESBService {
	
	private final static Logger log = Logger.getLogger(YESBServiceImpl.class);
	
	@Override
	public BSInfo parseYESB1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering YESService parseYES with request : " + request);
		
		String filepath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Statement\\s*of\\s*account\\s*:\\s*(\\d*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(YESB\\w{7})"));
			String name = CommonUtils.extractField(text, "Primary\\s*Holder\\s*:(.*)A\\/C").replaceAll("\\s+", " ");
			if(name.equalsIgnoreCase("") || name.equalsIgnoreCase("NA")) {
				name = CommonUtils.extractField(text, "Period\\s*:.*\\n\\s*(.*)\\s*Your\\s*Branch\\s*Details").replaceAll("\\s+", " ").trim();
			}
			bankStatementInfo.setName(name);
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Period.*\\n([\\s\\S]*?)\\s*Mobile", 100).replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*details.*\\n.*Name\\s*:(.*)").replaceAll("\\s+", " ").trim());
			String dateFormat = "dd-MMM-yyyy";
			String[] period =CommonUtils.extractMultiGroupArray(text, "Period\\s*:\\s*From\\s*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
			if(period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			} else {
				period = CommonUtils.extractMultiGroupArray(text, "Period\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), "ddMMMyyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), "ddMMMyyyy"));
			}
			bankStatementInfo.setTransactions(extracTransactionsYESB_1(filepath, bankStatementInfo.getAccountNo(), dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in YESBService parseYESB1: " + e);
		}
		log.info("Exiting YESService parseYESB1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for YESService parseYESB1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseYESB2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering YESBServiceImpl parseYESB2 with request : " + request);

		String filepath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "A/C\\s*type\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setName(CommonUtils.extractField(text, "\\s*(.*)\\s*OD\\s*Limit").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*Id\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "A/C\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "OD\\s*Limit.*\\n([\\s\\S]*?)(?=A/C)", 80));

			String dateFormat = "dd-MMM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*:\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*:\\s*.*?To\\s*(\\S*)"), dateFormat));

			bankStatementInfo.setTransactions(extracTransactionsYESB_2(text, bankStatementInfo.getAccountNo(), dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in YESBServiceImpl parseYESB2: " + e);
		}
		log.info("Exiting YESBServiceImpl parseYESB2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for YESBServiceImpl parseYESB2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseYESB3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering YESBService parseYESB3 with request : " + request);
		
		String filepath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Statement\\s*of\\s*account\\s*:\\s*(\\d*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(YESB\\w{7})"));
			bankStatementInfo.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*:(.*)Your\\s*Branch").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(\\s*Add.*Line1[\\s\\S]*?)\\n\\s*Mobile\\s*No", 80)
					.replaceAll("Line[0-9]\s*:", "").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*:\\s*(\\S*)"));
			String[] period =CommonUtils.extractMultiGroupArray(text, "Statement\\s*Type\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			if(period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), "ddMMMyyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), "ddMMMyyyy"));
			}
			bankStatementInfo.setTransactions(extracTransactionsYESB_3(text, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in YESBService parseYESB3: " + e);
		}
		log.info("Exiting YESService parseYESB1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for YESBService parseYESB3 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extracTransactionsYESB_1(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		Pattern dateFormat1 = Pattern.compile("\\d{2}\\s[A-Za-z]{3}\\s\\d{4}");
		Pattern dateFormat2 = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{4}");
		String txnDateFormat = "";
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				
				if (data.length >= 6 && !data[0].equalsIgnoreCase("Transaction Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					
					String date = data[0];
					if(txnDateFormat.equalsIgnoreCase("")) {
						Matcher dateMatcher1 = dateFormat1.matcher(date);
						Matcher dateMatcher2 = dateFormat2.matcher(date);
						
						if(dateMatcher1.find()) {
							txnDateFormat= "dd MMM yyyy";
						}else if(dateMatcher2.find()) {
							txnDateFormat = "dd-MMM-yyyy";
						}
					}
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], txnDateFormat)); 
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], txnDateFormat));
					transaction.setDescription(data[3]); 
					transaction.setTxnId(data[2]);

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

	private List<Transaction> extracTransactionsYESB_2(String pdfText, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile(
				"(\\d{2}-\\w{3}-\\d{4})\\s*(\\d{2}-\\w{3}-\\d{4})\\s*(.{50}).{20}\\s*([\\d,-\\.]+)\\s*([\\d,\\.]+)\\s*([\\d,\\.]+)([\\s\\S]*?)(?=(\\d{2}-\\w{3}-\\d{4})|Page|Opening)");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
			transaction.setDescription(
					(matcher.group(3) + " " + matcher.group(7)).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());

			if (matcher.group(4).equals("0.00")) { // credit
				transaction.setCredit(matcher.group(5));
				transaction.setDebit("");
				transaction.setAmount(transaction.getCredit());
				transaction.setTxnType("CREDIT");
			} else if (matcher.group(4).contains("-")) { // credit
				transaction.setCredit(matcher.group(4).replace("-", ""));
				transaction.setDebit("");
				transaction.setAmount(transaction.getCredit());
				transaction.setTxnType("CREDIT");
			} else {
				transaction.setDebit(matcher.group(4));
				transaction.setCredit("");
				transaction.setAmount(transaction.getDebit());
				transaction.setTxnType("DEBIT");
			}
			transaction.setBalance(matcher.group(6));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}
	
//	
	
	private List<Transaction> extracTransactionsYESB_3(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("\\s*Statement\\s*of\\s*account[\\s\\S]*?Transaction\\s*ValueDate[\\s\\S]*?Date.*", "");
		String[] lines = pdfText.split("\n");
		
		Pattern pattern = Pattern.compile(
				"(\\d{4}-\\d{2}-\\d{2})\\s*(\\d{4}-\\d{2}-\\d{2})\\s*(.*)\\s{5,}\\S+\\s+(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s*)(-?\\d*,?\\d*,?\\d+\\.\\d+)");
		
		int serialNoCount = 1;
		Transaction transaction = new Transaction();
		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			if(matcher.find()) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
					transaction = new Transaction();
				}
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setTxnDate(matcher.group(1));
				transaction.setValueDate(matcher.group(2));
				transaction.setDescription(matcher.group(3).replaceAll("\\s+", " ").trim());
				transaction.setAccNo(accountNo);
				transaction.setBalance(matcher.group(6));
				String amount = matcher.group(4);
				String spaces = matcher.group(5);
				if(spaces.length() > 15) {
					transaction.setDebit(amount);
					transaction.setTxnType("DEBIT");
				}else {
					transaction.setCredit(amount);
					transaction.setTxnType("CREDIT");
				}
				transaction.setAmount(amount);
			}else if(transaction != null){
				transaction.setDescription(transaction.getDescription() + line.trim().split("\\s+")[0].replaceAll("\\s+", " ").trim());
			}
		}
		transactions.add(transaction);
		return transactions;
	}
}
