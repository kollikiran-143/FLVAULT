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
public class SIBServiceImpl implements SIBService{
	
	private final static Logger log = Logger.getLogger(HDFCServiceImpl.class);
	
	@Override
	public BSInfo parseSIB1(ParseBankStmtRequestDTO request) {
		log.info("Entering SIBServiceImpl parseSIB1 with request: " + request);
		long startTimeInMillis = System.currentTimeMillis();

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String pdfFilePath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");
			String accountNo = CommonUtils.extractField(text, "[Aa]*/[Cc]*\\s*[Nn]*[oO]*\\s*:\\s*(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.{50})"));
			bankStatementInfo
					.setAccountType((CommonUtils.extractField(text, "TYPE\\s*:\\s*(.*)")).replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)").trim());
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text,
					"FROM\\s*(\\d{2}-\\d{2}-\\d{4})\\s*[tToO]*\\s*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null && period.length >= 2) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			String[] details = extractAddressSIB_1_2_3(text, "Ph:\\s*([\\s\\S]*?)\\s*(?=^\\s*STATEMENT|\\s*Nominee)");

			if (details.length >= 2) {
				String address = details[0];
				String gmail = Arrays.stream(address.split("\\s+"))
						.filter(s -> s.contains("@GMAIL") || s.contains("@gmail") || s.contains("@Gmail"))
						.reduce((first, second) -> second).orElse("");
				address = address.replaceFirst(Pattern.quote(gmail), "").trim();

				bankStatementInfo.setAddress(address.trim());
				bankStatementInfo.setName(details[1].trim());
				bankStatementInfo.setEmail(gmail.trim());
			}
			List<Transaction> transactions = extractTransactionsSIB_1(text, accountNo);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in SIBServiceImpl parseSIB1: ", e);
		}

		log.info("Exiting SIBServiceImpl parseSIB1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SIBServiceImpl parseSIB1 is ==> " + timeTaken);
		return bankStatementInfo;
	}
	
	public BSInfo parseSIB2(ParseBankStmtRequestDTO request) {
		log.info("Entering SIBServiceImpl parseSIB2 with request: "+ request);
	    long startTimeInMillis = System.currentTimeMillis();
	    
	    BSInfo bankStatementInfo = new BSInfo();
	    try {
	        String pdfFilePath = request.getFileName();
	        String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");

	        String accountNo = CommonUtils.extractField(text, "A/C\\s*NO\\s*:\\s*(\\d+)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
	        bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.*)"));
	        bankStatementInfo.setAccountType((CommonUtils.extractField(text, "TYPE\\s*:\\s*(.*)")).replaceAll("\\s+"," "));
	        bankStatementInfo.setEmail(CommonUtils.extractField(text, "EMAIL\\s*:\\s*(.*)\\s*(?=Mode)"));
	        String dateFormat = "dd-MM-yyyy";
	        String[] period = CommonUtils.extractMultiGroupArray(text, "FROM\\s*(\\d{2}-\\d{2}-\\d{4})\\s*to\\s*(\\d{2}-\\d{2}-\\d{4})");
	        if(period != null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
	        String[] details = extractAddressSIB_1_2_3(text, "Ph:\\s*([\\s\\S]*?)\\s*(?=CURRENCY)");
	        if(details != null && details.length>=2) {
		        bankStatementInfo.setAddress(details[0].trim());
		        bankStatementInfo.setName(details[1].trim());
	        }
	        List<Transaction> transactions = extractTransactionsSIB_2(text, accountNo);
	        bankStatementInfo.setTransactions(transactions);
		        
	    } catch (Exception e) {
//	    	e.printStackTrace();
	        log.error("Error in SIBServiceImpl parseSIB2: ", e);
	    }

	    log.info("Exiting SIBServiceImpl parseSIB2: " + bankStatementInfo);
	    long timeTaken = System.currentTimeMillis() - startTimeInMillis;
	    log.info("Time Taken for SIBServiceImpl parseSIB2 is ==> " + timeTaken);
	    return bankStatementInfo;
	}

	public BSInfo parseSIB3(ParseBankStmtRequestDTO request) {
		log.info("Entering SIBServiceImpl parseSIB3 with request: "+ request);
	    long startTimeInMillis = System.currentTimeMillis();
	    
	    BSInfo bankStatementInfo = new BSInfo();
	    try {
	        String pdfFilePath = request.getFileName();
	        String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");

	        String accountNo = CommonUtils.extractField(text, "A/C\\s*No\\s*:\\s*(\\d+)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
	        bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.*)"));
	        bankStatementInfo.setAccountType(CommonUtils.extractField(text, "TYPE\\s*:\\s*(.*)").replaceAll("\\s+", " "));
	        String dateFormat = "dd-MM-yyyy";
	        String[] period = CommonUtils.extractMultiGroupArray(text, "FROM\\s*(\\d{2}-\\d{2}-\\d{4})\\s*to\\s*(\\d{2}-\\d{2}-\\d{4})");
	        if(period != null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
	        String[] details = extractAddressSIB_1_2_3(text, "Ph:\\s*([\\s\\S]*?)\\s*(?=Mode)");
	        if(details.length>=2) {
		        bankStatementInfo.setAddress(details[0].trim());
		        bankStatementInfo.setName(details[1].trim());
	        }
	        List<Transaction> transactions = extractTransactionsSIB_3(text, accountNo);
	        bankStatementInfo.setTransactions(transactions);
	        
	    } catch (Exception e) {
//	    	e.printStackTrace();
	        log.error("Error in SIBServiceImpl parseSIB3: ", e);
	    }

	    log.info("Exiting SIBServiceImpl parseSIB3: " + bankStatementInfo);
	    long timeTaken = System.currentTimeMillis() - startTimeInMillis;
	    log.info("Time Taken for SIBServiceImpl parseSIB3 is ==> " + timeTaken);
	    return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseSIB4(ParseBankStmtRequestDTO request) {
		log.info("Entering SIBServiceImpl parseSIB4 with request: " + request);
		long startTimeInMillis = System.currentTimeMillis();

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String pdfFilePath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(pdfFilePath, "3");
			String accountNo = CommonUtils.extractField(text, "[Aa]*/[Cc]*\\s*[Nn]*[oO]*\\s*:\\s*(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType((CommonUtils.extractField(text, "TYPE\\s*:\\s*(.*)")).replaceAll("\\s+", " "));
//			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Moblie\\s*Number\\s:\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)\\s*DATE\\s*:\\s*\\d{2}-\\d{2}-\\d{4}").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "DATE\\s*:\\s*\\d{2}-\\d{2}-\\d{4}.*\\n([\\s\\S]*)?\\n\\s*Regd\\.\\s*Mobile", 60));
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "PERIOD\\s*(\\d{2}-\\d{2}-\\d{4})\\s*[tToO]*\\s*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null && period.length >= 2) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsSIB_4(text, accountNo, dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in SIBServiceImpl parseSIB4: ", e);
		}

		log.info("Exiting SIBServiceImpl parseSIB4: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SIBServiceImpl parseSIB4 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	private String[] extractAddressSIB_1_2_3(String text, String regex) {
//		Pattern pattern = Pattern.compile("Ph:\\s*([\\s\\S]*?)\\s*(?=CURRENCY)", Pattern.DOTALL);
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(text);
		String name = "";
		String address = "";
		if (matcher.find()) {
			int flag = 0;
			String lines = matcher.group();
			String[] parts = lines.split("\n");
			for (int i = 1; i < parts.length; i++) {
				String part = parts[i];
				if (part.length() >= 77) {
					address += " ";
					address += (part.substring(0, 77)).trim();
					address += " ";
				} else if (part.length() > 0) {
					address += " ";
					address += (part.substring(0, part.length())).trim();
				}

				if (flag == 0) {
					if (part.length() >= 77) {
						name += (part.substring(0, 77)).trim();
						name += " ";
						flag = 1;
					} else if (part.length() > 0) {
						name += " ";
						name += (part.substring(0, part.length())).trim();
						flag = 1;
					}
				}
			}
		}
		return new String[] { address.replaceAll("\\s+", " "), name.replaceAll("\\s+", " ") };
	}
	
	private List<Transaction> extractTransactionsSIB_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        Pattern transactionPattern = Pattern.compile("^\\s*\\d{2}-\\d{2}-\\d{2}.*\\n[\\s\\S]*?(?=\\s*Page|\\s*\\d{2}-\\d{2}-\\d{2})", Pattern.MULTILINE);
        Matcher matcher = transactionPattern.matcher(text);
        long serialNoCount = 0;
        Transaction prevTrans = null;
        while(matcher.find()) {
            Transaction transaction = new Transaction();
            serialNoCount++;
            String[] parts = (matcher.group()).split("\n");
            String descrition = "";
            String txnDate = "";
            int flag = 0;
            for(String part : parts) {
                if(part.length()<=40 && prevTrans!=null) {
                    prevTrans.setDescription(prevTrans.getDescription()+part.substring(0,part.length()).trim());
                    break;
                }
                if(part.length()>=22) {
                    txnDate += part.substring(0,22).trim();
                }
                if(part.length()>=29 && part.length()<=84) {
                    descrition += part.substring(26).trim();
                } else if(part.length()>=185) {
                    descrition += part.substring(26,76).trim();
                    transaction.setDebit(part.substring(107,139).trim());
                    transaction.setCredit(part.substring(140,169).trim());
                    transaction.setBalance(part.substring(170,185).trim().replaceAll("[A-Za-z]*", ""));
                } 
            }

            if(transaction.getDebit() != null && !transaction.getDebit().equalsIgnoreCase("")) {
                transaction.setAmount(transaction.getDebit());
                transaction.setCredit(null);
                transaction.setTxnType("DEBIT");
                flag = 1;
            } else if(transaction.getCredit() != null && !transaction.getCredit().equalsIgnoreCase("")){
                transaction.setAmount(transaction.getCredit());
                transaction.setDebit(null);
                transaction.setTxnType("CREDIT");
                flag = 1;
            }
            if(flag == 1) {
                transaction.setsNo(String.valueOf(serialNoCount));
                transaction.setAccNo(accountNo);
                transaction.setTxnDate(CommonUtils.dateFormatter(txnDate,"dd-MM-yy"));
                transaction.setDescription(descrition.replaceAll("\\s+", " "));
                transactions.add(transaction);
                prevTrans = transaction;
            } else {
            	serialNoCount--;
            }
        }
        return transactions;
    }

	private List<Transaction> extractTransactionsSIB_2(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");
        Pattern transactionPattern = Pattern.compile("(\\d{2}-\\d{2}-\\d{2})\\s*(.*)\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})");

        Pattern continuationPattern = Pattern.compile("^(.+)$");
        Pattern end = Pattern.compile("(Summary|Page|-----------)");
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
	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd-MM-yy"));  
	                currentTransaction.setDescription(matcher.group(2).replaceAll("\\s+"," ")); 
	
	                String firstAmount = matcher.group(3) != null ? matcher.group(3): "";
	                String balance = matcher.group(4) != null ? matcher.group(4) : "";
	
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
		
	private List<Transaction> extractTransactionsSIB_3(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");
        Pattern transactionPattern = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+(\\d*)\\s+\\s*([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})");

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
	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1),"dd-MM-yyyy"));  
	                currentTransaction.setDescription(matcher.group(2).replaceAll("\\s+"," ")); 
	
	                String firstAmount = matcher.group(4) != null ? matcher.group(4): "";
	                String balance = matcher.group(5) != null ? matcher.group(5) : "";
	
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
	
	
	private List<Transaction> extractTransactionsSIB_4(String text, String accountNo, String dateFormat) {
        List<Transaction> transactions = new ArrayList<>();
        text = text.replaceAll("\\s*Page\\s*Total[\\s\\S]*?DEPOSITS\\s*BALANCE", "");
        Pattern txnPattern = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})\\s{5,}(.{60})\\s*(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s*)(-?\\d*,?\\d*,?\\d+\\.\\d+)\\s*Cr");
        Pattern startPattern = Pattern.compile("DATE\\s*PARTICULARS\\s*CHQ.*BALANCE");
        Pattern pdfEndPattern = Pattern.compile("Grand\\s*Total\\s*:");
        long serialNoCount = 1;
        
        boolean start = false;
        
        String[] lines = text.split("\\n");
        Transaction transaction = new Transaction();
        String desc = "";
        for (String eachLine : lines) {
			Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
			Matcher startMatcher = startPattern.matcher(eachLine);
			Matcher txnMatcher = txnPattern.matcher(eachLine);
			if(pdfEndMatcher.find()) {
				break;
			}
			if(startMatcher.find()) {
				start = true;
				continue;
			}
			if(start) {
				if(txnMatcher.find()) {
					desc = (!desc.equalsIgnoreCase("") ? desc : "") + txnMatcher.group(2);
					desc = desc.replaceAll("\\s+", " ").trim();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1), dateFormat));
					transaction.setDescription(desc);
					String balance = txnMatcher.group(5) != null ? txnMatcher.group(5) : "";
					transaction.setBalance(balance);
					String whites = txnMatcher.group(4);
					String amount = txnMatcher.group(3);
					if(whites.length() > 25) {
						transaction.setDebit(amount);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					}else {
						transaction.setCredit(amount);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setAmount(amount);
					transactions.add(transaction);
					transaction = new Transaction();
					desc = "";
				}else {
					desc = eachLine;
				}
			}
		}
        return transactions;
    }
}