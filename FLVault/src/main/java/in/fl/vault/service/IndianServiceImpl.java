package in.fl.vault.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class IndianServiceImpl implements IndianService{
	
	private final static Logger log = Logger.getLogger(IndianServiceImpl.class);
	
	@Override
	public BSInfo parseINDIAN1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
        log.info("Entering IndianServiceImpl parseINDIAN1 with request: " + request);
        
        String filePath = request.getFileName();
        BSInfo bankStatementInfo = new BSInfo();
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
        	bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No.\\s*:\\s*(\\S*)"));
            bankStatementInfo.setName(CommonUtils.extractField(pdfText, "BRANCH\\s*CODE.*\\n\\s*(.*)").replaceAll("\\s+"," "));
            bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "STATEMENT\\s*OF\\s* ACCOUNT.*\\n([\\s\\S]*?)\\n\\s*Nomination", 80));
            bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "EMAIL\\s*ID\\s*:\\s*(\\S*)"));
            bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "BRANCH\\s*CODE\\s*:\\s*(\\S*)"));
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*From\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
            String dateFormat = "dd/MM/yyyy";
            if(period != null) {
                bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
                bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            bankStatementInfo.setTransactions(extractTransactionsINDIAN_1(pdfText, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in IndianServiceImpl parseINDIAN1: "+e);
		}
        log.info("Exiting IndianServiceImpl parseINDIAN1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndianServiceImpl parseINDIAN1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseINDIAN2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndianServiceImpl parseINDIAN2 with request: " + request);

		String filePath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No.\\s*:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(IDIB\\w{7})"));

			Matcher addressMatcher = Pattern
					.compile("STATEMENT\\s*OF.*([\\s\\S]*?)(?=^\\s*Account\\s*No)", Pattern.MULTILINE).matcher(pdfText);
			if (addressMatcher.find()) {
				String[] nameAddress = addressMatcher.group(1).split("\\n");
				nameAddress = Arrays.stream(nameAddress).map(String::trim).filter(line -> !line.isEmpty())
						.toArray(String[]::new);
				if (nameAddress.length > 1) {
					String name = nameAddress[0];
					name = (name.length() > 40 ? name.substring(0, 40) : name).trim();
					name = name.replaceAll("\\s+", " ");
					bankStatementInfo.setName(name);
					String address = "";
					for (int i = 1; i < nameAddress.length; i++) {
						String line = nameAddress[i];
						address += (line.length() > 40 ? line.substring(0, 40) : line);
					}
					address = address.replaceAll("\\s+", " ").trim();
					bankStatementInfo.setAddress(address);
				}
			}

			String dateFormat = "dd-MMM-yyyy";

			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*name\\s*:(.{0,40})"));
			bankStatementInfo.setStartDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(pdfText, "Statement\\s*Date\\s*:\\s*(.*)"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(pdfText, "Statement\\s*From\\s*:\\s*(.*)"), dateFormat));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Code\\s*:\\s*(.*)"));

			bankStatementInfo.setTransactions(extractTransactionsINDIAN_2(pdfText, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in IndianServiceImpl parseINDIAN2: " + e);
		}
		log.info("Exiting IndianServiceImpl parseINDIAN2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndianServiceImpl parseINDIAN2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsINDIAN_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");
        Pattern transactionPattern = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(\\d{2}\\/\\d{2}\\/\\d{2})\\s*([\\s\\S]*?)\\s{15,}(\\S+)(\\s+)(\\S+)[CD]r");
        Pattern end = Pattern.compile("(CARRIED\\s*FORWARD|END\\s*OF\\s*STATEMENT)");
        Transaction currentTransaction = null;
        long serialNoCount = 1;
        for (String line : lines) {
            Matcher pageEnd = end.matcher(line.trim());

            if(!pageEnd.find()) {
                Matcher matcher = transactionPattern.matcher(line.trim());

	            if (matcher.find()) {
	                currentTransaction = new Transaction();
	            	currentTransaction.setsNo(String.valueOf(serialNoCount++));
	            	currentTransaction.setAccNo(accountNo);
	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd/MM/yy"));
	                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2),"dd/MM/yy"));  
	                currentTransaction.setDescription(matcher.group(3).replaceAll("\\s+"," "));
	                
	                String amount =  matcher.group(4) != null ? matcher.group(4): "";
	                String whitespaces = matcher.group(5);
	                String balance = matcher.group(6) != null ? matcher.group(6) : "";
	
	                if (whitespaces.length()>30) {
	                    currentTransaction.setDebit(amount);
	                    currentTransaction.setTxnType("DEBIT");
	                } else {
	                    currentTransaction.setCredit(amount);
	                    currentTransaction.setTxnType("CREDIT");
	                }
	                currentTransaction.setAmount(amount);
	                currentTransaction.setBalance(balance);
	                transactions.add(currentTransaction);
	            } else if (currentTransaction != null) {                    
	                currentTransaction.setDescription((currentTransaction.getDescription() + " " + line.replaceAll("\\s+", " ")).trim());
	            } 
        	} else {
	            	currentTransaction = null;
        	}
        }
        return transactions;
    }
	
	
	private List<Transaction> extractTransactionsINDIAN_(String text, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();
		text = text.replaceAll("\\n\\s*Carried\\s*Forward[\\s\\S]*?\\n\\s*Brought\\s*Forward.*", "");
		Matcher transactionmatcher = Pattern
				.compile("(^\\s*\\d{2}/\\d{2}/\\d{2}[\\s\\S]*?)(?=^\\s*\\d{2}/\\d{2}/\\d{2}|^\\s*CLOSING)",
						Pattern.MULTILINE)
				.matcher(text);
		long serialNoCount = 1;
		while (transactionmatcher.find()) {
			Transaction currentTransaction = new Transaction();

			String[] lines = transactionmatcher.group(1).split("\\n");
			String credit = "", debit = "", balance = "", txnDate = "", valueDate = "", description = "";

			for (String line : lines) {
				txnDate += " " + (line.length() >= 25 ? line.substring(0, 25) : line);
				valueDate += " " + (line.length() >= 42 ? line.substring(25, 42)
						: (line.length() >= 25 ? line.substring(25) : ""));
				description += " " + (line.length() >= 107 ? line.substring(42, 107)
						: (line.length() >= 42 ? line.substring(42) : ""));
				debit += " " + (line.length() >= 144 ? line.substring(126, 144)
						: (line.length() >= 126 ? line.substring(126) : ""));
				credit += " " + (line.length() >= 162 ? line.substring(144, 162)
						: (line.length() >= 144 ? line.substring(144) : ""));
				balance += " " + (line.length() >= 162 ? line.substring(162) : "");

			}
			txnDate = txnDate.replaceAll("\\s*", "");
			valueDate = valueDate.replaceAll("\\s*", "");
			description = description.replaceAll("\\s+", " ").trim();
			debit = debit.replaceAll("\\s*", "");
			credit = credit.replaceAll("\\s*", "");
			balance = balance.replaceAll("\\s+", "").replaceAll("[CDcdrR]", "");
			if (debit.isEmpty()) {
				currentTransaction.setAmount(credit);
				currentTransaction.setTxnType("CREDIT");
			} else {
				currentTransaction.setAmount(debit);
				currentTransaction.setTxnType("DEBIT");
			}

			currentTransaction.setsNo(String.valueOf(serialNoCount++));
			currentTransaction.setAccNo(accountNo);
			currentTransaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd/MM/yy"));
			currentTransaction.setValueDate(CommonUtils.dateFormatter(valueDate, "dd/MM/yy"));
			currentTransaction.setDescription(description);
			currentTransaction.setDebit(debit);
			currentTransaction.setCredit(credit);
			currentTransaction.setBalance(balance);
			transactions.add(currentTransaction);

		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsINDIAN_2(String pdfText, String accountNo) {
	
		//(\d{2}\/\d{2}\/\d{2})\s*(\d{2}\/\d{2}\/\d{2})\s*.{5,40}
		//(\d{2}\/\d{2}\/\d{2})\s*(\d{2}\/\d{2}\/\d{2})\s*.{5,40}.*?(-?\d*,?\d*,?\d+\.\d+)(\s*)(-?\d*,?\d*,?\d+\.\d+)[C|D]r
		//(.*)(-?\d*,?\d*,?\d+\.\d+)(\s*)(-?\d*,?\d*,?\d+\.\d+)[C|D]r
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("\\n\\s*Carried\\s*Forward[\\s\\S]*?\\n\\s*Brought\\s*Forward.*", "")
				.replaceAll("\\n\\s*STATEMENT\\s*OF\\s*ACCOUNT[\\s\\S]*?\\n\\s*Date\\s*Details.*Balance", "");
		try {
			
			Pattern txnPattern1 = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(.{5,40}).*?(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s*)(-?\\d*,?\\d*,?\\d+\\.\\d+)[C|D]?r?");
			Pattern txnPattern2 = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(.{5,40})");
			Pattern txnPattern3 = Pattern.compile("(.{5,40}).*?(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s*)(-?\\d*,?\\d*,?\\d+\\.\\d+)[C|D]?r?");
			Pattern pdfEndPattern = Pattern.compile("CLOSING\\s*BALANCE");
			Pattern txnStart = Pattern.compile("Brought\\s*Forward");
			boolean txnstatus= false;
			
			String[] lines = pdfText.split("\\n");
			int serialNoCount = 1;
			String dateFormat = "dd/MM/yy";
			Transaction transaction = new Transaction();
			for (String eachLine: lines) {
//				System.out.println(eachLine);
				eachLine = eachLine.trim();
				Matcher txnStartMatcher = txnStart.matcher(eachLine);
				Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
				if(txnStartMatcher.find()) {
					txnstatus = true;
					continue;
				}
				if(pdfEndMatcher.find()) {
					if(transaction.getTxnDate() != null) {
						transactions.add(transaction);
					}
					break;
				}
				if(txnstatus) {
					Matcher txnMatcher1 = txnPattern1.matcher(eachLine);
					Matcher txnMatcher2 = txnPattern2.matcher(eachLine);
					Matcher txnMatcher3 = txnPattern3.matcher(eachLine);
					if(txnMatcher1.find()) {
						if(transaction.getTxnDate() != null) {
							transaction.setDescription(transaction.getDescription().replaceAll("\\s+", " ").trim());
							transactions.add(transaction);
							transaction = new Transaction();
						}
						transaction.setsNo(Integer.toString(serialNoCount++));
						transaction.setAccNo(accountNo);
						transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher1.group(1), dateFormat));
						transaction.setValueDate(CommonUtils.dateFormatter(txnMatcher1.group(2), dateFormat));
						transaction.setDescription(txnMatcher1.group(3));
						transaction.setBalance(txnMatcher1.group(6));
						String amount = txnMatcher1.group(4);
						transaction.setAmount(amount);
						String whiteSpaces = txnMatcher1.group(5);
						if(whiteSpaces.length() > 10) {
							transaction.setDebit(amount);
							transaction.setTxnType("DEBIT");
							transaction.setCredit("");
						}else {
							transaction.setCredit(amount);
							transaction.setTxnType("CREDIT");
							transaction.setDebit("");
						}
					}else if(txnMatcher2.find()) {
						if(transaction.getTxnDate() != null) {
							transactions.add(transaction);
							transaction = new Transaction();
						}
						transaction.setsNo(Integer.toString(serialNoCount++));
						transaction.setAccNo(accountNo);
						transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher2.group(1), dateFormat));
						transaction.setValueDate(CommonUtils.dateFormatter(txnMatcher2.group(2), dateFormat));
						transaction.setDescription(txnMatcher2.group(3));
					}else if(txnMatcher3.find()) {
						String desc = (transaction.getDescription() != null && !transaction.getDescription().equals("")) ? transaction.getDescription() : "";
						desc += txnMatcher3.group(1);
						desc= desc.replaceAll("\\s+", " ").trim();
						transaction.setDescription(desc);
						transaction.setBalance(txnMatcher3.group(4));
						String amount = txnMatcher3.group(2);
						transaction.setAmount(amount);
						String whiteSpaces = txnMatcher3.group(3);
						if(whiteSpaces.length() > 10) {
							transaction.setDebit(amount);
							transaction.setTxnType("DEBIT");
							transaction.setCredit("");
						}else {
							transaction.setCredit(amount);
							transaction.setTxnType("CREDIT");
							transaction.setDebit("");
						}
					}else {
						String desc = (transaction.getDescription() != null && !transaction.getDescription().equals("")) ? transaction.getDescription() : "";
						desc += eachLine;
						desc= desc.replaceAll("\\s+", " ").trim();
						transaction.setDescription(desc);
					}
				}
			}
		} catch (Exception e) {
			log.error("Error in extractTransactionsINDIAN_2"+ e);
		}
		return transactions;
	}
}
