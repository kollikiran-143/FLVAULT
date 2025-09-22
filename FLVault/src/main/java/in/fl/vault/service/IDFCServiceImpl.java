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
public class IDFCServiceImpl implements IDFCService{
	
	private final static Logger log = Logger.getLogger(IDFCServiceImpl.class);
	
	@Override
	public BSInfo parseIDFC1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDFCServiceImpl parseIDFC1 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            
            String accountNo = CommonUtils.extractField(pdfText, "ACCOUNT\\s*NO\\s*:\\s*(\\d+)");
            bankStatementInfo.setAccountNo(accountNo);
            
            bankStatementInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "(CUSTOMER\\s*NAME\\s*:[\\s\\S]*?)\\n\\s*COMMUNICATION", 105).replaceAll("CUSTOMER\\s*NAME\\s*:\\s*", ""));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "(COMMUNICATION\\s*:[\\s\\S]*?)\\n\\s*EMAIL", 105).replaceAll("COMMUNICATION\\s*:\\s*", ""));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "ACCOUNT\\s*BRANCH\\s*:\\s*(.*)").replaceAll("\\s+", " "));
            bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*([\\w\\d]+)"));
            bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "ACCOUNT\\s*TYPE\\s*:\\s*([\\w\\s]*)\\n").replaceAll("\\s+", " "));
            bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "EMAIL\\s*ID\\s*:\\s*([A-Za-z].{50})"));
            bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "PHONE\\s*NO\\s*:\\s*([\\*\\d]*)"));
            bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "NOMINATION\\s*:(.{50})").replaceAll("\\s+", " "));
            
            String dateFormat = "yyyy-MM-dd";
            String txnDateFormat = "dd-MMM-yyyy";
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "STATEMENT\\s*PERIOD\\s*:\\s*(\\d{4}-\\d{2}-\\d{2})\\s*TO\\s*(\\d{4}-\\d{2}-\\d{2})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsIDFC_1(filePath, accountNo, txnDateFormat);
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in IDFCServiceImpl parseIDFC1: "+e);
        }
        
        log.info("Exiting IDFCServiceImpl parseIDFC1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDFCServiceImpl parseIDFC1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIDFC2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDFCServiceImpl parseIDFC2 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
            
            String accountNo = CommonUtils.extractField(pdfText, "ACCOUNT\\s*NO\\s*:\\s*(\\d*)");
            bankStatementInfo.setAccountNo(accountNo);
            
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "(.*)ACCOUNT\\s*BRANCH").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "STATEMENT\\s*FOR.*\\n([\\s\\S]*?)\\n\\s*EMAIL", 75));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "ACCOUNT\\s*BRANCH\\s*:\\s*(.*)").replaceAll("\\s+", " "));
            bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(IDFB\\w{7})"));
            bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "ACCOUNT\\s*TYPE\\s*:\\s*(.*)").replaceAll("\\s+", " "));
            bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "EMAIL\\s*ID\\s*:\\s*([\\w\\*@\\.]*)"));
            bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "PHONE\\s*NO\\s*:\\s*(.{40})").replaceAll("\\s+", " "));
            
            String dateFormat = "dd-MMM-yyyy";
            String txnDateFormat = "dd MMM yy HH:mm";   // 01 May 24 08:13
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "STATEMENT\\s*FOR\\s*(\\d{2}-[A-Z]{3}-\\d{4})\\s*to\\s*(\\d{2}-[A-Z]{3}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsIDFC_2(filePath, accountNo, txnDateFormat);
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in IDFCServiceImpl parseIDFC2: "+e);
        }
        log.info("Exiting IDFCServiceImpl parseIDFC2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDFCServiceImpl parseIDFC2 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	public BSInfo parseIDFC3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDFCServiceImpl parseIDFC3 with request :" + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
			
			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"STATEMENT\\s*FOR\\s*(\\d{2}-\\w{3}-\\d{4})\\s*TO\\s*((\\d{2}-\\w{3}-\\d{4}))");
			String dateformat = "dd-MMM-yyyy";
			if(period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateformat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateformat));
			}
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(IDFB\\w{7})"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "ACCOUNT\\s*TYPE\\s*:\\s*(.*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "EMAIL\\s*ID\\s*:\\s*([\\w\\*@\\.]*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "PHONE\\s*NO\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "ACCOUNT\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "ACCOUNT\\s*NO\\s*:\\s*(\\d*)"));
			bankStatementInfo.setAddress(
					CommonUtils.extractMultiLinesField(pdfText, "ACCOUNT\\s*:.*\\n([\\s\\S]*?)\\n\\s*EMAIL", 58));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "(.*?)ACCOUNT\\s*:").replaceAll("\\s+", " "));
			bankStatementInfo.setTransactions(extractTransactionsIDFC_3(filePath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in IDFCServiceImpl parseIDFC3: " + e);
		}

		log.info("Exiting IDFCServiceImpl parseIDFC3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDFCServiceImpl parseIDFC3 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsIDFC_1(String filePath, String accountNo, String dateFormat) throws IOException{
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
			    Pattern datePattern = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{4}");

				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
				if(dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);
					transaction.setBalance(data[6]);
					if (data[4] != null && !data[4].equalsIgnoreCase("") && !data[4].equals("-")) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsIDFC_2(String filePath, String accountNo, String dateFormat) throws IOException{
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				Pattern datePattern = Pattern.compile("\\d{2}\\s[A-Za-z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}");
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
				if(dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], "dd MMM yy"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[2]);
					
					String balance = data[6].replaceAll("CR", "").trim();
					transaction.setBalance(balance);
					if (data[4] != null && !data[4].equalsIgnoreCase("") && !data[4].equals("-")) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsIDFC_3(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");

				if (data.length == 7 && !data[2].equals("Opening Balance") && !data[1].equals("Value Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0].substring(0, 7), "dd/MM/yy"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], "dd/MM/yy"));
					transaction.setDescription(data[2]);
					transaction.setBalance(data[6].replaceAll("[CD]r", "").trim());
					
					if (data[4] != null && !data[4].equalsIgnoreCase("") && !data[4].equals("-")) {
						transaction.setAmount(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setDebit(data[4]);
						transaction.setCredit("");
					} else if(data[5] != null && !data[5].equalsIgnoreCase("") && !data[5].equals("-")){
						transaction.setAmount(data[5]);
						transaction.setTxnType("CREDIT");
						transaction.setCredit(data[5]);
						transaction.setDebit("");
					}else if(transaction.getDescription().contains("CHQ Issued Bounce")){
						transaction.setAmount("0");
						transaction.setTxnType("DEBIT");
						transaction.setDebit("0");
						transaction.setDebit("");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}
