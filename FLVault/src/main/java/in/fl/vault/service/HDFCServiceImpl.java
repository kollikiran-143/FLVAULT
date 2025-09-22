package in.fl.vault.service;
import java.io.IOException;
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
public class HDFCServiceImpl implements HDFCService{
	
	private final static Logger log = Logger.getLogger(HDFCServiceImpl.class);

	@Override
    public BSInfo parseHDFC1(ParseBankStmtRequestDTO request) {
    	log.info("Entering HDFCServiceImpl parseHDFC1 with request: "+ request);
		long startTimeInMillis = System.currentTimeMillis();
		
		String filePath = request.getFileName();
        BSInfo bankStatementInfo = new BSInfo();
        
        try {            
            String text = CommonUtils.extractTextFromPdf(filePath, "3");
            
            String[] period = CommonUtils.extractMultiGroupArray(text, "\\(From:\\s*(\\d{2}-\\d{2}-\\d{4})\\s*To:\\s*(\\d{2}-\\d{2}-\\d{4})\\)");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], "dd-MM-yyyy"));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], "dd-MM-yyyy"));
            }
            String accountNo = CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*([X\\d]+)");
            
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setName(CommonUtils.extractField(text, "CUSTOMER\\s*NAME\\s*(.+)"));
            bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*\\s*(.+)"));
            bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*CODE\\s*\\s*([A-Z\\d]+)"));
            bankStatementInfo.setPan(CommonUtils.extractField(text, "PAN\\s*([A-Z\\d]+)"));
            bankStatementInfo.setDob(CommonUtils.dateFormatter(CommonUtils.extractField(text, "DATE\\s*OF\\s*BIRTH\\s*([\\d-]+)"),"dd-MM-yyyy"));
            bankStatementInfo.setEmail(CommonUtils.extractField(text, "EMAIL\\s*(.+)"));
            bankStatementInfo.setPhone1(CommonUtils.extractField(text, "MOBILE\\s*(\\d+)"));
            bankStatementInfo.setAddress(CommonUtils.extractField(text, "CUSTOMER\\s*ADDRESS\\s*(.+?)(?=DATE\\s*OF\\s*BIRTH)", Pattern.DOTALL).replaceAll("\\s+", " "));
            bankStatementInfo.setAccountType(CommonUtils.extractField(text, "ACCOUNT\\s*TYPE\\s*\\s*(.+)"));
            List<Transaction> transactions = extractTransactionsHDFC_1(text, accountNo);
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in HDFCServiceImpl parseHDFC1 is: "+e);
        }

        log.info("Exiting HDFCServiceImpl parseHDFC1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for HDFCServiceImpl parseHDFC1 is ==>" + timeTaken);
        return bankStatementInfo;
    }
	
	@Override
    public BSInfo parseHDFC2(ParseBankStmtRequestDTO request) throws IOException {
    	log.info("Entering HDFCServiceImpl parseHDFC2 with request: "+ request);
		long startTimeInMillis = System.currentTimeMillis();
		
		Pattern format1 = Pattern.compile("\\d+\\s+0.00\\s+\\d+\\,?\\d*\\.\\d+");
		
		String filePath = request.getFileName();
        BSInfo bankStatementInfo = new BSInfo();
        
        try {            
            String text = CommonUtils.extractTextFromPdf(filePath, "4");
	
	        // Extract the required fields using regex
        	String name = CommonUtils.extractField(text, "(.*)City").replaceAll("\\s+", " ").trim();
	        String accountNo = CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*(\\d+)");
	        String ifsc = CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\w+)");
	        String branch = CommonUtils.extractField(text, "Account\\s*Branch\\s*:\\s*([\\S]+)");
	        String email = CommonUtils.extractField(text, "Email\\s*:\\s*([\\w\\.]+@[\\w\\.]+)");
	        String phone = CommonUtils.extractField(text, "Phone\\s*no.\\s*:\\s*([\\d/]+)");
	        String nominee = CommonUtils.extractField(text, "Nomination\\s*:\\s*(\\w+)");
	        String start = CommonUtils.extractField(text, "From\\s*:\\s*([\\S]+)");
	        String end = CommonUtils.extractField(text, "To\\s*:\\s*([\\S]+)");
	        String address = CommonUtils.extractMultiLinesField(text, "Address\\s{2,}:.*\\n([\\s\\S]*?)\\s*JOINT\\s*HOLDERS", 85);
	        
	        bankStatementInfo.setName(name);
            bankStatementInfo.setAddress(address);
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setIfsc(ifsc);
	        bankStatementInfo.setBranch(branch);
	        bankStatementInfo.setEmail(email);
	        bankStatementInfo.setPhone1(phone);
	        bankStatementInfo.setNominee(nominee);
	        bankStatementInfo.setStartDate(CommonUtils.dateFormatter(start,"dd/MM/yyyy"));
	        bankStatementInfo.setEnDate(CommonUtils.dateFormatter(end, "dd/MM/yyyy"));
	        
	        List<Transaction> transactions = new ArrayList<>();
	        Matcher formatMatcher = format1.matcher(text);
	        if(formatMatcher.find()) {
	        	log.info("Calling extractTransactionsHDFC_2_1");
	        	transactions = extractTransactionsHDFC_2_1(text, accountNo);
	        }else {
	        	log.info("Calling extractTransactionsHDFC_2_2");
	        	transactions = extractTransactionsHDFC_2_2(text, accountNo);
	        }
	        bankStatementInfo.setTransactions(transactions);

        } catch(Exception e) {
//        	e.printStackTrace();
        	log.error("Error in HDFCServiceImpl parseHDFC2 is: "+e);
        }

        log.info("Exiting HDFCServiceImpl parseHDFC2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for HDFCServiceImpl parseHDFC2 is ==>" + timeTaken);
        return bankStatementInfo;
    }
	
	@Override
	public BSInfo parseHDFC3(ParseBankStmtRequestDTO request) {
		log.info("Entering HDFCServiceImpl parseHDFC3 with request: "+ request);
		long startTimeInMillis = System.currentTimeMillis();
		
		String filePath = request.getFileName();
        BSInfo bankStatementInfo = new BSInfo();
        
        try {            
            String text = CommonUtils.extractTextFromPdf(filePath, "4");
            
            String accountNo = CommonUtils.extractField(text, "Account\\s*number\\s*:\\s*(\\S*)");
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setBranch(CommonUtils.extractField(text, "Account\\s*Branch\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
            bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*(HDFC\\w*)"));
            bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*:\\s*(.*)"));
            //for address & name
            Pattern pattern = Pattern.compile("\\s+Address.*\\n([\\s\\S]*?)\\n\\s*JOINT");
    		Matcher matcher = pattern.matcher(text);
    		if (matcher.find()) {
    			String[] arr = matcher.group(1).split("\n");
    			String result = "";
    			int i= 0;
    			for (String eachLine : arr) {
//    				System.out.println(eachLine);
    				if (eachLine.length() > 80) {
    					result += (eachLine.substring(0, 80) + " ");
    				} else {
    					result += (eachLine + " ");
    				}
    				if(result.replaceAll("\\s+", " ").trim().isEmpty()) {
    					continue;
    				}
    				if(i==0) {
    					bankStatementInfo.setName(result.replaceAll("\\s+", " ").trim());
    					i++;
    				}
    			}
    			bankStatementInfo.setAddress(result.replaceAll("\\s+", " ").trim());
    		}
            String dateFormat = "dd/MM/yyyy";
            String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}\\/\\d{2}\\/\\d{2}).*(\\d{2}\\/\\d{2}\\/\\d{2})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsHDFC_3(filePath, accountNo);
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in HDFCServiceImpl parseHDFC3 is: "+e);
        }

        log.info("Exiting HDFCServiceImpl parseHDFC3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for HDFCServiceImpl parseHDFC3 is ==>" + timeTaken);
        return bankStatementInfo;
	}

    private List<Transaction> extractTransactionsHDFC_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        Pattern transactionPattern = Pattern.compile(
            "(\\d{2}-\\d{2}-\\d{4})\\s+(\\d+)\\s+(.+?)\\s+(\\d+\\.\\d{2})\\s+(\\d+\\.\\d{2})"
        );
        Pattern continuationPattern = Pattern.compile("(\\S+)(?:\\s+(.+))?");
        
        Transaction currentTransaction = null;
        long serialNoCount = 1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = transactionPattern.matcher(line);
            
            if (matcher.find()) {
                currentTransaction = new Transaction();
                currentTransaction.setsNo(String.valueOf(serialNoCount++));
                currentTransaction.setAccNo(accountNo);
                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd-MM-yyyy"));
                currentTransaction.setTxnId(matcher.group(2));
                currentTransaction.setDescription(matcher.group(3).trim());
                
                String firstAmount = matcher.group(4);
                String secondAmount = matcher.group(5);

                int descEnd = line.indexOf(matcher.group(3)) + matcher.group(3).length();
                int firstNumberStart = line.indexOf(firstAmount);
                int secondNumberStart = line.indexOf(secondAmount);
                
                if ((firstNumberStart - descEnd) < (secondNumberStart - firstNumberStart)) {
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
                    String txnId = continuationMatcher.group(1);
                    String additionalDescription = continuationMatcher.group(2) != null ? continuationMatcher.group(2).trim() : "";
                    currentTransaction.setTxnId(currentTransaction.getTxnId() + txnId);
                    currentTransaction.setDescription(currentTransaction.getDescription() + (additionalDescription.isEmpty() ? "" : additionalDescription));
                    currentTransaction = null;
                }
            }
        }
        return transactions;
    }

    private List<Transaction> extractTransactionsHDFC_2_2(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        Pattern transactionPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})\\s+(.+?)\\s+(\\d{2}/\\d{2}/\\d{2})\\s+([\\d,]*\\.\\d{2})?\\s*([\\d,]*\\.\\d{2})?\\s+([\\d,]*\\.\\d{2})");
        Pattern continuationPattern = Pattern.compile("^(.+)$");

        Transaction currentTransaction = null;
        long serialNoCount=1;
        for (String line : lines) {
            Matcher matcher = transactionPattern.matcher(line);

            if (matcher.find()) {
                currentTransaction = new Transaction();
                currentTransaction.setsNo(String.valueOf(serialNoCount++));
                currentTransaction.setAccNo(accountNo);
                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd/MM/yy")); 
                currentTransaction.setDescription((matcher.group(2).trim()).replaceAll("\\s+", " ")); 
                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(3),"dd/MM/yy")); 
                String firstAmount = matcher.group(4) != null ? matcher.group(4): "";
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
                currentTransaction.setBalance(balance);
                currentTransaction.setAmount(firstAmount);

                transactions.add(currentTransaction);

            } else if (currentTransaction != null) {
                Matcher continuationMatcher = continuationPattern.matcher(line);
                if (continuationMatcher.find()) {
                    String additionalDescription = continuationMatcher.group(1).trim();
                    
                    currentTransaction.setDescription((currentTransaction.getDescription() + " " + additionalDescription).replaceAll("\\s+", " "));
                    currentTransaction = null;
                }
            }
        }
        return transactions;
    }
    
    private List<Transaction> extractTransactionsHDFC_2_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        Pattern transactionPattern = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(.+?)(\\d{16})\\s*(\\d{2}\\/\\d{2}\\/\\d{2})\\s*(\\S+)\\s*(\\S+)\\s*(\\S+)");

        Transaction currentTransaction = null;
        long serialNoCount=1;
        for (String line : lines) {
            Matcher matcher = transactionPattern.matcher(line);

            if (matcher.find()) {
                currentTransaction = new Transaction();
                currentTransaction.setsNo(String.valueOf(serialNoCount++));
                currentTransaction.setAccNo(accountNo);
                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd/MM/yy")); 
                currentTransaction.setDescription((matcher.group(2).trim()).replaceAll("\\s+", " ")); 
                currentTransaction.setTxnId(matcher.group(3));
                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(4),"dd/MM/yy")); 
                String debit = matcher.group(5);
                String credit = matcher.group(6);
                String balance = matcher.group(7);

                if (!debit.equalsIgnoreCase("0.00")) {
                    currentTransaction.setDebit(debit);
                    currentTransaction.setTxnType("DEBIT");
                    currentTransaction.setAmount(debit);
                    currentTransaction.setCredit("");
                } else {
                	currentTransaction.setCredit(credit);
                    currentTransaction.setTxnType("CREDIT");
                    currentTransaction.setAmount(credit);
                    currentTransaction.setDebit("");
                }
                currentTransaction.setBalance(balance);
                transactions.add(currentTransaction);

            } else if (currentTransaction != null) {
                currentTransaction.setDescription((currentTransaction.getDescription() + " " + line).replaceAll("\\s+", " "));
                currentTransaction = null;
            }
        }
        return transactions;
    }
    
    private List<Transaction> extractTransactionsHDFC_3(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		String dateFormat = "dd/MM/yyyy";
		Pattern datePattern = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
				if (dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat)); 
					transaction.setDescription(data[1]);
					transaction.setTxnId(data[2]);
					transaction.setValueDate(CommonUtils.dateFormatter(data[3], dateFormat));
					String debit = data[4];
					String credit = data[5];
					
					if(debit !=null && !debit.equalsIgnoreCase("") && !debit.equalsIgnoreCase("0.00")) {
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
					transaction.setBalance(data[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}