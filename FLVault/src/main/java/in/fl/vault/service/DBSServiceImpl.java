package in.fl.vault.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.apache.log4j.Logger;
import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class DBSServiceImpl implements DBSService{
	
	private final static Logger log = Logger.getLogger(HDFCServiceImpl.class);
	
	@Override
	public BSInfo parseDBS1(ParseBankStmtRequestDTO request) {
	    log.info("Entering DBSServiceImpl parseDBS1 with request: "+ request);
	    long startTimeInMillis = System.currentTimeMillis();
	    
	    BSInfo bankStatementInfo = new BSInfo();
	    try {
	        String pdfFilePath = request.getFileName();
	        String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");

	        String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*Period\\s*:\\s*(\\d{2}-\\w{3}-\\d{4})\\s*To\\s*(\\d{2}-\\w{3}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0],"dd-MMM-yyyy"));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1],"dd-MMM-yyyy"));
            }
            
            String accountNo = CommonUtils.extractField(text, "Account\\s*No\\s*.:\\s*([X\\d]+)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*:\\s*(.+)").replaceAll("\\s+", " "));
	        bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*([A-Z0-9]+)"));
	        bankStatementInfo.setAccountType((CommonUtils.extractField(text, "Account\\s*Type\\s*:\\s*(.+)")).replaceAll("\\s+", " "));
	        bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Tel\\s*:\\s*((.+))"));
	        bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\s*No.*\\n([\\s\\S]*?)\\n.*Dear\\s*Customer", 100));
	        
	        List<Transaction> transactions = extractTransactionsDBS_1(text, accountNo);
	        bankStatementInfo.setTransactions(transactions);

	    } catch (Exception e) {
//	    	e.printStackTrace();
	        log.error("Error in DBSServiceImpl parseDBS1: ", e);
	    }

	    log.info("Exiting DBSServiceImpl parseDBS1: " + bankStatementInfo.printWithoutTrxs());
	    long timeTaken = System.currentTimeMillis() - startTimeInMillis;
	    log.info("Time Taken for DBSServiceImpl parseDBS1 is ==> " + timeTaken);
	    return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseDBS2(ParseBankStmtRequestDTO request) {
	    log.info("Entering DBSServiceImpl parseDBS2 with request: "+ request);
	    long startTimeInMillis = System.currentTimeMillis();
	    
	    BSInfo bankStatementInfo = new BSInfo();
	    try {
	        String pdfFilePath = request.getFileName();
	        String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");
	        
	        String accountNo = CommonUtils.extractField(text, "Account\\s*number\\s*\\:\\s*(\\d+)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*code\\s*(\\S*)"));
	        bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*name\\s*:\\s*(.*)"));
	        bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*from\\s*:\\s*(.*)"),"dd/MM/yyyy"));
	        bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*to\\s*:\\s*(.*)"),"dd/MM/yyyy"));
	        List<Transaction> transactions = extractTransactionsDBS_2(text, accountNo);
	        bankStatementInfo.setTransactions(transactions);
	        String[] accountDetails = extractAccountDetailsDBS_2(text);
	        if(accountDetails.length>=4) {
	        	bankStatementInfo.setName(accountDetails[0].replaceAll("\\s+", " "));
	        	bankStatementInfo.setAddress(accountDetails[1].replaceAll("\\s+", " "));
	        	bankStatementInfo.setBranch(accountDetails[2].replaceAll("\\s+", " "));
	        	bankStatementInfo.setAccountType(accountDetails[3].replaceAll("\\s+", " "));
	        }
	    } catch (Exception e) {
//	    	e.printStackTrace();
	        log.error("Error in DBSServiceImpl parseDBS2: ", e);
	    }

	    log.info("Exiting DBSServiceImpl parseDBS2: " + bankStatementInfo.printWithoutTrxs());
	    long timeTaken = System.currentTimeMillis() - startTimeInMillis;
	    log.info("Time Taken for DBSServiceImpl parseDBS2 is ==> " + timeTaken);
	    return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseDBS3(ParseBankStmtRequestDTO request) {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering DBSServiceImpl parseDBS3 with request :" + request);
		
        BSInfo bankStatementInfo = new BSInfo();
        String filePath = request.getFileName();
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            String[] details = extractAccountDetailsDBS_3(pdfText);
            if(details.length>=3) {
            	bankStatementInfo.setName(details[0].replaceAll("\\s+", " ").trim());
            	bankStatementInfo.setAccountType(details[1].trim().replaceAll("\\s+", " "));
            	bankStatementInfo.setAccountNo(details[2]);
            }
            
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(\\d{2}-\\d{2}-\\d{4})\\s*To\\s*(\\d{2}-\\d{2}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0],"dd-MM-yyyy"));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1],"dd-MM-yyyy"));
            }
           
            List<Transaction> transactions = extractTransactionsDBS_3(pdfText, bankStatementInfo.getAccountNo());
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
	        log.error("Error in DBSServiceImpl parseDBS3: ", e);
	    }
	    log.info("Exiting DBSServiceImpl parseDBS3: " + bankStatementInfo.printWithoutTrxs());
	    long timeTaken = System.currentTimeMillis() - startTimeInMillis;
	    log.info("Time Taken for DBSServiceImpl parseDBS3 is ==> " + timeTaken);
	    return bankStatementInfo;
    }

	private List<Transaction> extractTransactionsDBS_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        // Adjust pattern to match DBS transaction entries (date, txnId, description, debit, credit, balance)
        Pattern transactionPattern = Pattern.compile(
            "(\\d{2}-[A-z]{3}-\\d{4})\\s+(\\d{2}-[A-z]{3}-\\d{4})\\s+(.+?)\\s+(\\d+(?:,\\d{3})*\\.\\d{2})\\s+(\\d+(?:,\\d{3})*\\.\\d{2})"
        );
        Pattern continuationPattern = Pattern.compile("^(.*)$");

        Transaction currentTransaction = null;
        int serialNoCount = 1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = transactionPattern.matcher(line);

            if (matcher.find()) {
                currentTransaction = new Transaction();
                
                currentTransaction.setsNo(String.valueOf(serialNoCount++));
                currentTransaction.setAccNo(accountNo);
                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd-MMM-yyyy")); // Transaction date
                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2),"dd-MMM-yyyy")); // Value date
                currentTransaction.setDescription(matcher.group(3).trim()); // Description

                // Determine debit/credit based on column positioning
                String debitOrCredit = matcher.group(4); // First amount (can be debit or credit)
                String balance = matcher.group(5); // Balance after transaction

                currentTransaction.setAmount(debitOrCredit); // Set the transaction amount
                currentTransaction.setBalance(balance); // Set the final balance

                // Simple heuristic: Check the description and placement to identify debit vs. credit
                int descEnd = line.indexOf(matcher.group(3)) + matcher.group(3).length();
                int firstNumberStart = line.indexOf(debitOrCredit);
                int secondNumberStart = line.indexOf(balance);

                if ((firstNumberStart - descEnd) < (secondNumberStart - firstNumberStart)) {
                    currentTransaction.setDebit(debitOrCredit);
                    currentTransaction.setTxnType("DEBIT");
                } else {
                    currentTransaction.setCredit(debitOrCredit);
                    currentTransaction.setTxnType("CREDIT");
                }

                transactions.add(currentTransaction);
            } else if (currentTransaction != null) {
                // Check for additional transaction descriptions or continuation lines
                Matcher continuationMatcher = continuationPattern.matcher(line);
                if (continuationMatcher.find()) {
                    String additionalDescription = continuationMatcher.group(1);
                    currentTransaction.setDescription((currentTransaction.getDescription() + " " + additionalDescription).replaceAll("\\s+", " "));
                    currentTransaction = null;
                }
            }
        }
        return transactions;
    }

	private List<Transaction> extractTransactionsDBS_2(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");
        Pattern transactionPattern = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s*(\\d{2}/\\d{2}/\\d{4})\\s*(\\d*)\\s*(.*)\\s+([\\d,]*.\\d{2})\\s+([\\d,]*.\\d{2})"
        );
        Pattern continuationPattern = Pattern.compile("^(.+)$");
        Pattern end = Pattern.compile("(Summary|Page|DBS\\s*Bank\\s*India\\s*Ltd)");
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
	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd/MM/yyyy"));  
	                currentTransaction.setDescription(matcher.group(4).replaceAll("\\s+"," ")); 
	                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2),"dd/MM/yyyy")); 
	
	                String firstAmount = matcher.group(5) != null ? matcher.group(5): "";
	                String balance = matcher.group(6) != null ? matcher.group(6) : "";
	
	                int firstNumberStart = line.indexOf(firstAmount);  
	                int secondNumberStart = line.indexOf(balance); 
	
	                if ((secondNumberStart - firstNumberStart)>30) {
	                    currentTransaction.setDebit(firstAmount);
	                    currentTransaction.setTxnType("DEBIT");
	                } else {
	                    currentTransaction.setCredit(firstAmount);
	                    currentTransaction.setTxnType("CREDIT");
	                }
	                
	                currentTransaction.setAmount(firstAmount);
	                currentTransaction.setBalance(balance);
	                transactions.add(currentTransaction);
	            } else if (currentTransaction != null) {
	                Matcher continuationMatcher = continuationPattern.matcher(line);
	                if (continuationMatcher.find()) {
	                    String additionalDescription = continuationMatcher.group(1).trim();	                    
	                    currentTransaction.setDescription((currentTransaction.getDescription() + " " + additionalDescription).replaceAll("\\s+", " "));
	                }
	            } 
        	} else {
	            	currentTransaction = null;
	            }
        }

        return transactions;
    }

	private List<Transaction> extractTransactionsDBS_3(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        
        String regex = "(^\\s*\\d{2}-\\d{2}-\\d{4})([\\s\\S]*?)\\s+([\\d,]*.\\d{2})\\n+(.*)(?=^\\s*\\d{2}-\\d{2}-\\d{4}|^\\s*DBS\\s*Bank\\s*Ltd)";
        Pattern transactionPattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = transactionPattern.matcher(text);
        int serialNoCount = 1;
        while (matcher.find()) {
            Transaction transaction = new Transaction();
            
            transaction.setsNo(String.valueOf(serialNoCount++));
            transaction.setAccNo(accountNo);
            String date = matcher.group(1).trim();
            transaction.setTxnDate(CommonUtils.dateFormatter(date,"dd-MM-yyyy"));
            int start = matcher.start(2);  
            int end = matcher.start(3);   
            int distance = end - start;
            
            String amount = matcher.group(3).replace(",", "").trim();
            transaction.setAmount(amount);
                        
            if (distance < 220) {
                transaction.setTxnType("DEBIT");
                transaction.setDebit(amount);
            } else {
                transaction.setTxnType("CREDIT");
                transaction.setCredit(amount);
            }
            
            String description = matcher.group(2).trim();
            transaction.setDescription(description.trim().replaceAll("\\s+", " "));
            
            transactions.add(transaction);
        }

        return transactions;
    }
    
	private String[] extractAccountDetailsDBS_2(String text) {
    	String[] result = {"", "", "", ""};
    	Pattern namePattern = Pattern.compile("Name\\s*:\\s*[\\s\\S]*\\s*Address", Pattern.DOTALL);
        Matcher matcher = namePattern.matcher(text);
        if(matcher.find()) {
	        String lines = matcher.group();
	        String[] parts = lines.split("\\n+");
	        for(String part : parts) {
	    		if(part.length()>=81) {
	    			result[0] += (part.substring(23,81).replaceAll(":","")).trim();
	    		} else if(part.length()>=32){
	    			result[0] += (part.substring(32,part.length())).trim();
	    		}
	    	}
        }
    	Pattern addressPattern = Pattern.compile("Address\\s*:\\s*[\\s\\S]*\\s*Branch\\s*code/Name", Pattern.DOTALL);
        matcher = addressPattern.matcher(text);
        if(matcher.find()) {
	        String lines = matcher.group();
	        String[] parts = lines.split("\\n+");
	        for(String part : parts) {
	    		if(part.length()>=81) {
	    			result[1] += (part.substring(23,81).replaceAll(":","")).trim();
	    		} else if(part.length()>=32){
	    			result[1] += (part.substring(32,part.length())).trim();
	    		}
	    	}
        }
    	Pattern branchPattern = Pattern.compile("Branch\\s*code/Name\\s*:\\s*[\\s\\S]*\\s*Branch\\s*address", Pattern.DOTALL);
        matcher = branchPattern.matcher(text);
        if(matcher.find()) {
	        String lines = matcher.group();
	        String[] parts = lines.split("\\n+");
	        for(String part : parts) {
	    		if(part.length()>=81) {
	    			result[2] += (part.substring(23,81).replaceAll(":","")).trim();
	    		} else if(part.length()>=32){
	    			result[2] += (part.substring(32,part.length())).trim();
	    		}
	    	}
        }
    	Pattern typePattern = Pattern.compile("Account\\s*name\\s*(.*)\\s*Account\\s*statement", Pattern.DOTALL);
    	matcher = typePattern.matcher(text);
        if(matcher.find()) {
	        String lines = matcher.group();
	        String[] parts = lines.split("\\n+");
	        for(String part : parts) {
	    		if(part.length()>=96) {
	    			result[3] += (part.substring(57,96).replaceAll("\\s+"," ")).trim();
	    			break;
	    		}
	    	}
        }
    	return result;
	}

	private String[] extractAccountDetailsDBS_3(String text) {
    	Pattern pattern = Pattern.compile("Nominee\\s*(.*?)\\s*Transaction", Pattern.DOTALL);
        Matcher nomineeMatcher = pattern.matcher(text);
        String name="", number="", type="";
        if (nomineeMatcher.find()) {
            String nomineeText = nomineeMatcher.group();
            String[] parts = nomineeText.split("\\n+"); 
            for(String part : parts) {
                if(part.length()>=95) {
                    name += (part.substring(0, 39)).trim();
                    type += (part.substring(39,78)).trim();
                    number += (part.substring(79,95)).trim();
                } else if(part.length()>=94) {
                    name += (part.substring(0, 39)).trim();
                    type += (part.substring(39,78)).trim();
                    number += (part.substring(79,94)).trim();
                }
            }

        }
        return new String[]{name, type, number};
    }
	
}