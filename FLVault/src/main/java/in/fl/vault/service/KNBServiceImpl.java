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
public class KNBServiceImpl implements KNBService{
	
	private final static Logger log = Logger.getLogger(StmtServiceImpl.class);

	@Override
	public BSInfo parseKNB1(ParseBankStmtRequestDTO request) throws IOException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KNBServiceImpl parseKNB1 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		List<Transaction> transactions = new ArrayList<>();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            
            Pattern transactionPattern = Pattern.compile("^\s+(\\d{2}-\\d{2}-\\d{4}.*)", Pattern.MULTILINE);
    		Matcher transactionMatcher = transactionPattern.matcher(pdfText);

    		bsInfo.setName(CommonUtils.extractField(pdfText, "Name(.*?)(?=Branch)").replaceAll("\\s+"," ").trim());
    		bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s+Id\\s+(.*?)\\s+").trim());
    		bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s+Name(.*)").replaceAll("\\s+"," ").trim());
    		bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(KARB\\w{7})"));
    		String dateFormat = "dd-MM-yyyy";
    		String[] accNoPeriod = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s+for\\s+A\\/c\\s*(\\d*)\\s*Between\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
    		if(accNoPeriod != null) {
    			bsInfo.setAccountNo(accNoPeriod[0]);
    			bsInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1], dateFormat));
    			bsInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2], dateFormat));
    		}
    		bsInfo.setPhone1(CommonUtils.extractField(pdfText,"(?:\\n|\\r\\n)\\s+Phone\\s+(.*)(?=\\s{10,})").trim());
	    	bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Phone", 100));
    		

    		List<String> regions = new ArrayList<>();
    		while (transactionMatcher.find()) {
    			regions.add(transactionMatcher.group());
    		}
    		int serialNoCount = 1;
    		for (String region : regions) {
    			
    			String description = "";
    			String date = "";
    			String debit = "";
    			String credit = "";
    			String balance = "";
    			Transaction transaction = new Transaction();

    			if (region.length() < 175) {
//    				System.out.println("Region length is insufficient for full extraction.");
//    				System.out.println(region);
    			} else {
    				date = region.substring(0, 30).trim();
    				description = region.substring(31, 106).trim();
    				debit = region.substring(134,155).trim();
    				credit = region.substring(156, 175).trim();
    				balance = region.substring(175).trim();
    			}
    			
    			if(credit.length()>0){
    				transaction.setTxnType("CREDIT");
    				transaction.setAmount(credit);
    			} else {
    				transaction.setTxnType("DEBIT");
    				transaction.setAmount(debit);
    			}
    			transaction.setAccNo(bsInfo.getAccountNo());
    			transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MM-yyyy"));
    			transaction.setBalance(balance);
    			transaction.setCredit(credit);
    			transaction.setDebit(debit);
    			transaction.setDescription(description.replaceAll("\\s+", " ").trim());
    			transaction.setsNo(String.valueOf(serialNoCount++));
    			transactions.add(transaction);
    		}
    		bsInfo.setTransactions(transactions);
        }catch (Exception e) {
//          	e.printStackTrace();
        	log.error("Error in KNBServiceImpl parseKNB1: "+e);
        }
        
        log.info("Exiting KNBServiceImpl parseKNB1: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KNBServiceImpl parseKNB1 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseKNB2(ParseBankStmtRequestDTO request) throws IOException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KNBServiceImpl parseKNB2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		List<Transaction> transactions = new ArrayList<>();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            
    		Pattern addressPattern=Pattern.compile("(?:\\n|\\r\\n)\\s+(Address[\\s\\S]*?)(?=(\\n|\\r\\n)\\s+Phone)");   		
    		Matcher addressMatcher=addressPattern.matcher(pdfText);

    		bsInfo.setName(CommonUtils.extractField(pdfText, "Name(.*?)(?=Branch)").trim().replaceAll("\\s+", " "));
    		bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*(.*)\\s*(?=Email)").trim());
    		bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "A/c\\s*Number(.*)(?=Branch)").trim());
    		bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(.*)").trim());
    		bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s+Name(.*)").trim());
    		String dateFormat = "dd-MMM-yyyy";
    		bsInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(pdfText, "period\\s*:\\s*(.*)\\s+-").trim(), dateFormat));
    		bsInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils
    				.extractField(pdfText, "period\\s*:.*\\s+-(.*)").trim(), dateFormat));
    		bsInfo.setPhone1(CommonUtils.extractField(pdfText,"\\n\\s*Phone\\s*(\\S*)"));

    		if (addressMatcher.find()) {
    			String []addresslist = addressMatcher.group(1).split("\\n");
    			String address="";
    			for(String str: addresslist){
    				if(str.length()<86){
    					address+=" "+str.substring(20).trim();
    				}else {
    					address+=" "+str.substring(20,86).trim();
    				}
    			}
	    		address=address.replaceAll("\\n+", "");
	    		address=address.replaceAll("\\s+", " ");	
	    		bsInfo.setAddress(address.trim());
    		}

            String[] lines = pdfText.split("\n");

            Pattern transactionPattern = Pattern.compile(
                "(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})"
            );
            Pattern continuationPattern = Pattern.compile("^(.+)$");
            
            Transaction currentTransaction = null;
            long serialNoCount = 1;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher matcher = transactionPattern.matcher(line);
                
                if (matcher.find()) {
                    currentTransaction = new Transaction();
                    currentTransaction.setsNo(String.valueOf(serialNoCount++));
                    currentTransaction.setAccNo(bsInfo.getAccountNo());
                    currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd-MM-yyyy"));
                    currentTransaction.setDescription(matcher.group(2).trim());
                    
                    String firstAmount = matcher.group(3);
                    String secondAmount = matcher.group(4);

                    int firstNumberEnd = line.indexOf(firstAmount)+firstAmount.length();  
                    int secondNumberStart = line.indexOf(secondAmount); 

                    if ((secondNumberStart - firstNumberEnd)>25) {
                        currentTransaction.setDebit(firstAmount);
                        currentTransaction.setTxnType("DEBIT");
                    } else {
                        currentTransaction.setCredit(firstAmount);
                        currentTransaction.setTxnType("CREDIT");
                    }
                    currentTransaction.setBalance(secondAmount);
                    currentTransaction.setAmount(firstAmount);
                    transactions.add(currentTransaction);
                } else if (currentTransaction != null) {
                    Matcher continuationMatcher = continuationPattern.matcher(line);
                    if (continuationMatcher.find()) {
                        String additionalDescription = continuationMatcher.group(1) != null ? continuationMatcher.group(1).trim() : "";
                        currentTransaction.setDescription((currentTransaction.getDescription() + (additionalDescription.isEmpty() ? "" : additionalDescription)).replaceAll("\\s+", " "));
                        currentTransaction = null;
                    }
                }
            }
    		bsInfo.setTransactions(transactions);
        }catch (Exception e) {
//          	e.printStackTrace();
        	log.error("Error in KNBServiceImpl parseKNB2: "+e);
        }
        
        log.info("Exiting KNBServiceImpl parseKNB2: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KNBServiceImpl parseKNB2 is ==>" + timeTaken);
        return bsInfo;
	}

	@Override
	public BSInfo parseKNB3(ParseBankStmtRequestDTO request) {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KNBServiceImpl parseKNB3 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
        	
    		bsInfo.setName(CommonUtils.extractField(pdfText, "Name\\s*(.*)Branch\\s*Code").replaceAll("\\s+", " ").trim());
    		bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)A\\/c\\s*Type", 90));
    		bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No\\.\\s*(\\+?\\d*)"));
    		bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(KARB\\w{7})"));
    		bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.*)"));
    		bsInfo.setEmail(CommonUtils.extractField(pdfText, "\\n\\s{5,20}E-Mail\\s*ID(.*)").replaceAll("\\s+", " ").trim());
    		bsInfo.setAccountType(CommonUtils.extractField(pdfText, "A\\/c\\s*Type\\s*(.*)\\s*Address"));
    		String dateFormat = "dd-MM-yyyy";
    		String[] accNoperiod = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*for\\s*account\\s*number\\s*(\\d*)\\s*Between\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
            if (accNoperiod != null && accNoperiod.length>2) {
            	bsInfo.setAccountNo(accNoperiod[0]);
            	bsInfo.setStartDate(CommonUtils.dateFormatter(accNoperiod[1], dateFormat));
            	bsInfo.setEnDate(CommonUtils.dateFormatter(accNoperiod[2], dateFormat));
            }
            
            List<Transaction> transactions=extractTransactionsKNB_3(pdfText, bsInfo.getAccountNo(), dateFormat);
    		bsInfo.setTransactions(transactions);
        }catch (Exception e) {
//          	e.printStackTrace();
        	log.error("Error in KNBServiceImpl parseKNB3: "+e);
        }
        log.info("Exiting KNBServiceImpl parseKNB3: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KNBServiceImpl parseKNB3 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseKNB4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KNBServiceImpl parseKNB4 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");

			bsInfo.setName(
					CommonUtils.extractField(pdfText, "Name\\s*(.*)Branch\\s*Code").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText,
					"Branch\\s*Code.*\\n([\\s\\S]*?)(?=\\s*A/c\\s*Type)", 72));
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No\\.\\s*(\\+?\\d*)"));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(KARB\\w{7})"));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.*)"));
			bsInfo.setEmail(
					CommonUtils.extractField(pdfText, "E-\\s*Mail\\s*ID\\s*(\\S*)").replaceAll("\\s+", " ").trim());
			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "A/c\\s*Type\\s*(.{35})").replaceAll("\\s+", ""));
			String dateFormat = "dd-MM-yyyy";
			String[] accNoperiod = CommonUtils.extractMultiGroupArray(pdfText,
					"Statement\\s*for\\s*account\\s*number\\s*(\\d*)\\s*Between\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if (accNoperiod != null && accNoperiod.length > 2) {
				bsInfo.setAccountNo(accNoperiod[0]);
				bsInfo.setStartDate(CommonUtils.dateFormatter(accNoperiod[1], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(accNoperiod[2], dateFormat));
			}

			List<Transaction> transactions = extractTransactionsKNB_4(pdfText, bsInfo.getAccountNo(), dateFormat);
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//          	e.printStackTrace();
			log.error("Error in KNBServiceImpl parseKNB4: " + e);
		}
		log.info("Exiting KNBServiceImpl parseKNB4: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KNBServiceImpl parseKNB4 is ==>" + timeTaken);
		return bsInfo;
	}

	
	private List<Transaction> extractTransactionsKNB_3(String pdfText, String accountNo, String dateFormat){
		List<Transaction> transactions = new ArrayList<>();
		String txnPattern = "(\\d{2}-\\d{2}-\\d{4})\\s{3,}([\\s\\S]*?)\\s{3,}(\\S*)(\\s*)(\\S*)";
        int serialNoCount = 1;
        String[] lineArr = pdfText.split("\\r?\\n");
        for (String eachLine : lineArr) {
			Transaction transaction = new Transaction();
			String[] txnArr = CommonUtils.extractMultiGroupArray(eachLine, txnPattern);
			if(txnArr != null) {
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnArr[0], dateFormat));
				transaction.setDescription(txnArr[1]);
				String amount = txnArr[2];
				transaction.setAmount(amount);
				transaction.setBalance(txnArr[4]);
				
				String whitespaces = txnArr[3];
				if(whitespaces.length() > 20) {
					transaction.setDebit(amount);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				}else {
					transaction.setCredit(amount);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
				}
				transactions.add(transaction);
			}
		}
		return transactions;
	}	
	
	private List<Transaction> extractTransactionsKNB_4(String pdfText, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
		Pattern pattern = Pattern.compile(
				"(^\\s*\\d{2}-\\d{2}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\d{2}-\\d{4}|^\\s*Closing|^\\s*Date)", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;
		String firstLine = "";
		while (matcher.find()) {
			String description = firstLine, debit = "", credit = "", txnDate = "", balance = "";
			Transaction transaction = new Transaction();
			String lines[] = matcher.group(1).split("\\n");
			int length = lines.length;
			if (length > 0) {
				if (lines[0].length() > 116) {
					txnDate += lines[0].substring(0, 31).trim();
                    description += lines[0].substring(31, 79).trim();
                    debit += lines[0].substring(79, 97).trim();
                    credit += lines[0].substring(97, 116).trim();
                    balance += lines[0].substring(116).trim();
				} else {
					txnDate += lines[0].substring(0, 31).trim();
					description += lines[0].substring(31).trim();
				}
			}

			if (length > 1) {
				if (lines[1].length() > 116) {
					description += lines[1].substring(0, 80).trim();
					debit += lines[1].substring(80, 97).trim();
					credit += lines[1].substring(97, 116).trim();
					balance += lines[1].substring(116).trim();
				} else {
					description += lines[1].trim();
				}
			}

			if (length > 2) {
				firstLine = lines[2].trim();
			} else {
				firstLine = "";
			}

			if (debit.equals("")) {
				transaction.setTxnType("CREDIT");
				transaction.setAmount(credit);
			} else {
				transaction.setTxnType("DEBIT");
				transaction.setAmount(debit);
			}
			description = description.replaceAll("\\s+", "");
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setAccNo(accountNo);
			transaction.setTxnDate(txnDate);
			transaction.setDescription(description);
			transaction.setDebit(debit);
			transaction.setCredit(credit);
			transaction.setBalance(balance);

			transactions.add(transaction);
		}
		return transactions;
	}
}