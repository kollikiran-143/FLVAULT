package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class KOTAKServiceImpl implements KOTAKService{
	
	private final static Logger log = Logger.getLogger(KOTAKServiceImpl.class);
	
	@Override
	public BSInfo parseKOTAK1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {

    	long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KOTAKServiceImpl parseKOTAK1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*#\\s*([\\dX]+)");
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*#\\s*[\\dX]+\\s*(.*)").replaceAll("\\s+", " "));
            bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*(.*)").replaceAll("\\s+", " "));
            bankStatementInfo.setName(CommonUtils.extractField(pdfText, "\\d{2}\\s*\\w{3}\\s*\\d{4}\\s*(.*)\\s*CRN").replaceAll("\\s+", " "));
            bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s+([A-Z0-9]+)"));
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(\\d{2}\\s*\\w+\\s*\\d{4})\\s*-\\s*(\\d{2}\\s*\\w+\\s*\\d{4})");
            if(period != null && period.length>=2) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""),"ddMMMyyyy"));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""),"ddMMMyyyy"));
            }
            bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "CRN.*\\n([\\s\\S]*?)\\n\\s*#\\s*TRANSACTION", 90));
            List<Transaction> transactions = extractTransactionsKOTAK_1(pdfText, accountNo);
            bankStatementInfo.setTransactions(transactions);
        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in KOTAKServiceImpl parseKOTAK1: "+e);
        }
        
        log.info("Exiting KOTAKServiceImpl parseKOTAK1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KOTAKServiceImpl parseKOTAK1 is ==>" + timeTaken);
        return bankStatementInfo;
    }

	@Override
	public BSInfo parseKOTAK2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KOTAKServiceImpl parseKOTAK2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
	        
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*#\\s*(\\d{10})");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Variant\\s*(.*)").replaceAll("\\s+", " "));
	        bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*branch\\s*(.*?)\\n"));
	        bankStatementInfo.setName((CommonUtils.extractField(pdfText, "(.*?)\\n(.*?)(?=CRN)")).replaceAll("\\s+"," "));
	        bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*([A-Z0-9]+)"));
	        String[] details = extractAddressBranchKOTAK_2(pdfText,"Home\\s*branch\\s*([\\s\\S]*?)\\s*(?=Ref\\.No)");
	        if(details.length>=2) {
	        	bankStatementInfo.setAddress(details[0].trim());
	        	bankStatementInfo.setBranch(details[1]);
	        }
	        String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(\\d{2}\\s*\\w{3}\\s*,\\s*\\d{4})\\s*-\\s*(\\d{2}\\s*\\w{3}\\s*,\\s*\\d{4})");
	        if(period != null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0],"dd MMM, yyyy"));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1],"dd MMM, yyyy"));
	        }
	        List<Transaction> transactions = extractTransactionsKOTAK_2(pdfText, accountNo);
	        bankStatementInfo.setTransactions(transactions);
        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in KOTAKServiceImpl parseKOTAK2: "+e);
        }       
        log.info("Exiting KOTAKServiceImpl parseKOTAK2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KOTAKServiceImpl parseKOTAK2 is ==>" + timeTaken);
        return bankStatementInfo;
    }

	@Override
	public BSInfo parseKOTAK3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KOTAKServiceImpl parseKOTAK3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
		List<Transaction>transactions=new ArrayList<>();
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
        	
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*\\.\\s*(\\d*)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*(.*)").replaceAll("\\s+"," "));
	        bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Period.*\\n([\\s\\S]*?)\\n\\s*CRN\\s*No", 80));
	        bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(KKBK\\w{7})"));
	        bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
	        bankStatementInfo.setPan(CommonUtils.extractField(pdfText, "\\s{5,}PAN\\s+(.*)"));
	        String dateFormat = "dd-MMM-yy";
	        String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(.*)Period\\s*(\\d{2}-[A-Za-z]{3}-\\d{2}).*(\\d{2}-[A-Za-z]{3}-\\d{2})\\n.*Currency");
	        if(period != null && period.length>=3) {
	        	bankStatementInfo.setName(period[0].replaceAll("\\s+"," "));
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[2], dateFormat));
	        }
	        
	        int serialCount=0;
	        
	        Pattern txnPattern=Pattern.compile("(^\\s*\\d{2}-\\w{3}-\\d{2}[\\s\\S]*?)(?=^\\s*\\d{2}-\\w{3}-\\d{2}|\\s*Contd.|\\s*A1)", Pattern.MULTILINE);
		    Matcher txnMatcher=txnPattern.matcher(pdfText);
		    
			while(txnMatcher.find()) {
				String regions=txnMatcher.group(1);
				Transaction transaction=new Transaction();	
				String description="";
				String credit="";
				String debit="";
				String txnDate="";
				String balance="";
				if(serialCount==0) {
					serialCount++;
					continue;
				}
				String []lines=regions.split("\\n");
				for(int j=0;j<lines.length;j++) {
					if(j==0) {  
						lines[0]=lines[0].trim();
						txnDate=lines[0].substring(0,11).trim();
						description+=lines[0].substring(11,67).trim();
						credit=lines[0].substring(128,153).trim();
						debit=lines[0].substring(105,128).trim();
						balance=lines[0].substring(153).trim();
					}else {
						description+=lines[j];
					}					
				}
				//balance=balance.substring(0,balance.length()-5).trim();
				transaction.setsNo(String.valueOf(serialCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate,"dd-MMM-yy"));
				transaction.setDescription(description.replaceAll("\\s+"," "));
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				balance=balance.substring(0,balance.length()-4).trim();
				transaction.setBalance(balance);
				
				if(debit.equalsIgnoreCase("")) {
					transaction.setTxnType("CREDIT");
					transaction.setAmount(credit);
				}else {
					transaction.setCredit("");
					transaction.setTxnType("DEBIT"); 
					transaction.setAmount(debit);
				}
				transactions.add(transaction);
			}
			bankStatementInfo.setTransactions(transactions);
        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in KOTAKServiceImpl parseKOTAK3: "+e);
        }       
        log.info("Exiting KOTAKServiceImpl parseKOTAK3: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KOTAKServiceImpl parseKOTAK3 is ==> " + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseKOTAK4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KOTAKServiceImpl parseKOTAK4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
        	
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\d*)");
	        bankStatementInfo.setAccountNo(accountNo);
	        bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*:\\s*(.*)").replaceAll("\\s+"," "));
	        bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Currency.*\\n([\\s\\S]*?)\\n\\s*Date", 60));
	        bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(KKBK\\w{7})"));
	        String dateFormat = "dd-MM-yyyy";
	        String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(.*)Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
	        if(period.length>=3) {
	        	bankStatementInfo.setName(period[0].replaceAll("\\s+"," ").trim());
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[2], dateFormat));
	        }
	        Pattern formatPattern = Pattern.compile("\\(Cr\\).*\\(Cr\\)");
	        Matcher formatMatcher = formatPattern.matcher(pdfText);
	        List<Transaction> transactions = new ArrayList<>();
	        
	        if(formatMatcher.find()) {
	        	log.info("Calling extractTransactionsKOTAK_4_2");
	        	transactions = extractTransactionsKOTAK_4_2(pdfText, accountNo, dateFormat);
	        }else {
	        	log.info("Calling extractTransactionsKOTAK_4_1");
	        	transactions = extractTransactionsKOTAK_4_1(pdfText, accountNo, dateFormat);
	        }
	        bankStatementInfo.setTransactions(transactions);
        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in KOTAKServiceImpl parseKOTAK4: "+e);
        }       
        log.info("Exiting KOTAKServiceImpl parseKOTAK4: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KOTAKServiceImpl parseKOTAK4 is ==>" + timeTaken);
        return bankStatementInfo;
	}

	@Override
	public BSInfo parseKOTAK5(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KOTAKServiceImpl parseKOTAK5 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			bankStatementInfo.setName(CommonUtils.extractField( text, "Account\\s*Statement.*\\n(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*.\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*(.*)").replaceAll("\\s+", "").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Cust\\s*.\\s*Reln\\s*.\\s*No\\s*.\\s*(\\S*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Account\\s*Statement.*\\n([\\s\\S]*?)\\n\\s*Sl\\.No", 90));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name\\s*(.*)\\n?\\s*Sl\\.No").replaceAll("\\s+", " ").trim());
			String[] period = CommonUtils.extractMultiGroupArray(text,
					"Period\\s*From\\s*(\\d{2}/\\d{2}/\\d{4})\\s*To\\s*(\\d{2}/\\d{2}/\\d{4})");
			String dateFormat = "dd/MM/yyyy";

			if (period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bankStatementInfo.setTransactions(extractTransactionsKOTAK_6(text, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in KOTAKServiceImpl parseKOTAK5: " + e);
		}
		log.info("Exiting KOTAKServiceImpl parseKOTAK5: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KOTAKServiceImpl parseKOTAK5 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsKOTAK_1(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");
        
        try {
        	Pattern transactionPattern = Pattern.compile(
                "(\\d{2}\\s*\\w{3}\\s*\\d{4})\\s+(\\d{2}\\s*\\w{3}\\s*\\d{4})?\\s+(.+?)\\s+([+-]?\\s*[\\d,.]+\\.\\d{2})\\s+([\\d,.]+\\.\\d{2})"
            );

            Pattern continuationPattern = Pattern.compile("^\\s+(.+)$");
            Pattern end = Pattern.compile("Statement generated on");

            Transaction currentTransaction = null;
            long sNo = 1;

            for (int i = 0; i < lines.length; i++) {
            	String line = lines[i];
                Matcher matcher = transactionPattern.matcher(line.trim());
                Matcher pageEnd = end.matcher(line.trim());
                if(!pageEnd.find()) {
    	            if (matcher.find()) {
    	                if (currentTransaction != null) {
    	                    transactions.add(currentTransaction);
    	                }
    	                currentTransaction = new Transaction();
    	                currentTransaction.setsNo(String.valueOf(sNo++));
    	                currentTransaction.setAccNo(accountNo);
    	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1).replaceAll("\\s*", ""),"ddMMMyyyy")); 
//    	                currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2).replaceAll("\\s*", ""),"ddMMMyyyy")); 
    	                String description = matcher.group(3);
    	                if(description.length()>=42) {
    	                	description = (description.substring(0, 42)).trim();
    	                }
    	                currentTransaction.setDescription(description.replaceAll("\\s+", " ")); 	
    	                String amount = matcher.group(4).replace(",", "").trim();
    	                String balance = matcher.group(5).replace(",", "").trim();
    	
    	                if (amount.startsWith("-")) {
    	                    currentTransaction.setDebit(amount.replace("-", ""));
    	                    currentTransaction.setTxnType("DEBIT");
    	                    currentTransaction.setCredit("");
    	                    currentTransaction.setAmount(amount.replace("-", ""));
    	                } else {
    	                    currentTransaction.setCredit(amount.replace("+", ""));
    	                    currentTransaction.setTxnType("CREDIT");
    	                    currentTransaction.setDebit("");
    	                    currentTransaction.setAmount(amount.replace("+", ""));
    	                }
    	
    	                currentTransaction.setBalance(balance);
    	
    	            } else if (currentTransaction != null) {
    	                Matcher continuationMatcher = continuationPattern.matcher(line);
    	                if (continuationMatcher.find()) {
    	                    String additionalDescription = continuationMatcher.group(1).trim();
    	                    if(additionalDescription.length()>=33) {
    	                    	additionalDescription = additionalDescription.substring(33,additionalDescription.length());
    	                    } else {
    	                    	additionalDescription = "";
    	                    }
    	                    currentTransaction.setDescription((currentTransaction.getDescription() + " " + additionalDescription).replaceAll("\\s+", " "));
    	                }
    	            }
                } else {
                	i += 6;
                }
            }

            if (currentTransaction != null) {
                transactions.add(currentTransaction);
            }
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
        return transactions;
    }

    private String[] extractAddressBranchKOTAK_2(String text, String regex) {
        String[] result = new String[2];
    	Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        String address = "";
        String branch = "";
        if (matcher.find()) {
        	int flag = 0;
            String lines = matcher.group();
            String[] parts = lines.split("\\n+"); 
            for(int i=1;i<parts.length;i++) {
            	String part = parts[i];
            	if(part.length()>=56) {
	            	address += (part.substring(0,56)).trim();
            		address += " ";
            	} else if (part.length()>13){
            		address += " ";
	            	address += (part.substring(0,part.length())).trim();
            	}
            	if(flag==0 && part.length()>90) {
            		branch += (part.substring(90).trim());
            		flag = 1;
            	}
            }
        }
        result[0]= address.replaceAll("\\s+", " ");
        result[1] = branch.replaceAll("\\s+", " ");
        return result;
	}

    private List<Transaction> extractTransactionsKOTAK_2(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\n");

        Pattern transactionPattern = Pattern.compile(
            "(\\d{2}\\s*\\w*\\s*,\\s*\\d{4})\\s*(.*?)\\s*([+-]\\s*[\\d,.]+\\.\\d{2})\\s*([\\d,.]+\\.\\d{2})"
        );

        Pattern continuationPattern = Pattern.compile("^\\s+(.+)$");
        Pattern end = Pattern.compile("(SUMMARY|Page)");

        Transaction currentTransaction = null;
        long serialNoCount = 1;
        for (int i = 0; i < lines.length; i++) {
        	String line = lines[i];
            Matcher matcher = transactionPattern.matcher(line.trim());
            Matcher pageEnd = end.matcher(line.trim());
            if(!pageEnd.find()) {
	            if (matcher.find()) {
	                if (currentTransaction != null) {
	                    transactions.add(currentTransaction);
	                }
	                currentTransaction = new Transaction();
	                currentTransaction.setsNo(String.valueOf(serialNoCount++));
	                currentTransaction.setAccNo(accountNo);
	                currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1).replaceAll("\\s+", " "),"dd MMM,yyyy")); 
	                String description = matcher.group(2);
	                if(description.length()>=59) {
	                	description = (description.substring(0, 59)).trim();
	                }
	                currentTransaction.setDescription(description.replaceAll("\\s+", " ")); 
	
	                String amount = matcher.group(3).replace(",", "").trim();
	                String balance = matcher.group(4).replace(",", "").trim();
	
	                if (amount.startsWith("-")) {
	                    currentTransaction.setDebit(amount.replace("-", ""));
	                    currentTransaction.setTxnType("DEBIT");
	                    currentTransaction.setAmount(amount.replace("-", ""));
	                } else {
	                    currentTransaction.setCredit(amount.replace("+", ""));
	                    currentTransaction.setTxnType("CREDIT");
	                    currentTransaction.setAmount(amount.replace("+", ""));
	                }
	
	                currentTransaction.setBalance(balance);
	
	            } else if (currentTransaction != null) {
	                Matcher continuationMatcher = continuationPattern.matcher(line);
	                if (continuationMatcher.find()) {
	                    String additionalDescription = continuationMatcher.group(1).trim();
	                    currentTransaction.setDescription((currentTransaction.getDescription() + " " + additionalDescription).replaceAll("\\s+", " "));
	                }
	            }
            } else {
            	if (currentTransaction != null) {
                    transactions.add(currentTransaction);
                }
            	currentTransaction = null;
            }
        }

        return transactions;
    }

    private List<Transaction> extractTransactionsKOTAK_4_1(String pdfText, String accountNo, String dateFormat){
		List<Transaction> transactions = new ArrayList<>();
		
		pdfText = pdfText.replaceAll(".*?Period[\\s\\S]*?Deposit\\(Cr\\)\\s*Balance", "");
		
		String[] lines = Arrays.stream(pdfText.split("\\r?\\n"))
		        .filter(line -> !line.trim().isEmpty())
		        .toArray(String[]::new);
		
		Pattern linePattern1 = Pattern.compile("(\\d{2}-\\d{2}-\\d{0,4})\\s*(.{60})\\s*(\\S*)(\\s*)(\\S*?)\\([CD]r\\)");
		Pattern pdfEndPattern = Pattern.compile("Statement\\s*Summary");
		Pattern linePattern2 = Pattern.compile("^\\s*(\\d{4})\\s+(\\S+)");
        
        String txnDate = "";
        String desc = "";
        String credit = "";
        String debit = "";
        String balance = "";
        int serialNoCount = 1;
        
        for (String line : lines) {
			Matcher lineMatcher1 = linePattern1.matcher(line);
			Matcher lineMatcher2 = linePattern2.matcher(line);
			Matcher pdfEndMatcher = pdfEndPattern.matcher(line);
			boolean pdfEnd = pdfEndMatcher.find();
			
			if(lineMatcher1.find() || pdfEnd) {
				if(!txnDate.equalsIgnoreCase("") || pdfEnd) {
           		 	Transaction transaction = new Transaction();
                    transaction.setsNo(String.valueOf(serialNoCount++));
                    transaction.setAccNo(accountNo);
                    transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
                    transaction.setDescription(desc.replaceAll("\\s+(UPI|NEFTINW|IMPS)-\\d*", "").replaceAll("\\s+", " ").trim());
                    transaction.setCredit(credit);
                    transaction.setDebit(debit);
                    transaction.setBalance(balance);
                    if (debit != null && !debit.equalsIgnoreCase("")) {
    					transaction.setAmount(debit);
    					transaction.setTxnType("DEBIT");
    				} else {
    					transaction.setAmount(credit);
    					transaction.setTxnType("CREDIT");
    				}
                    transactions.add(transaction);
                    txnDate = desc = credit = debit = balance = "";
			    }
				if(pdfEnd) {
					break;
				}
				txnDate = lineMatcher1.group(1);
				desc= lineMatcher1.group(2);
				balance = lineMatcher1.group(5);
				String whitespaces = lineMatcher1.group(4);
				if(whitespaces.length() > 25) {
					debit = lineMatcher1.group(3);
				}else {
					credit = lineMatcher1.group(3);
				}
			} else if(lineMatcher2.find()) {
				System.out.println("called");
				txnDate += lineMatcher2.group(1);
				desc += lineMatcher2.group(2);
			} else {
				desc += line.replaceAll("\\s+", " ").trim();
			}
        }
        return transactions;
	}

    private List<Transaction> extractTransactionsKOTAK_4_2(String pdfText, String accountNo, String dateFormat)
			throws IOException, InterruptedException {
		List<Transaction> transactions = new ArrayList<>();
		
		pdfText = pdfText.replaceAll(".*Page[\\s\\S]*?Deposit\\(Cr\\)", "");
		
		String[] lines = pdfText.split("\\n");
		int serialNumCount = 1;
		
		Pattern datePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}.*\\([C|D]r\\).*\\([C|D]r\\)");
		for (String line : lines) {
			Matcher dateMatcher = datePattern.matcher(line);
			Transaction transaction = new Transaction();

			String txnDate = "", amount="", desc = "", balance = "";
			if(dateMatcher.find()) {
				line = line.replaceFirst("^\\s+", "").replaceAll("\\s{2,}", "##");
//				System.out.println(line);
				String[] lineArr = line.split("##");
				
				for (int j = 0; j < lineArr.length; j++) {
					if(j==0) {
						txnDate = lineArr[0];
					}else if(j == lineArr.length -2) {	
						amount = lineArr[j];
					}else if(j == lineArr.length -1) {
						balance = lineArr[j];
					}else {
						desc += lineArr[j];
					}
				}
//				System.out.println(txnDate + " | " + amount +" | "+ balance);
				if(amount.contains("Dr")) {
					transaction.setTxnType("DEBIT");
					amount = amount.substring(0, amount.length() - 4);
					transaction.setDebit(amount);
				} else {
					transaction.setTxnType("CREDIT");
					amount = amount.substring(0, amount.length() - 4);
					transaction.setCredit(amount);
				}
				transaction.setAmount(amount);
				transaction.setsNo(Integer.toString(serialNumCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
				transaction.setAccNo(accountNo);
				transaction.setBalance(balance.substring(0, balance.length()-4));
				transactions.add(transaction);
			}else {
				transaction.setDescription(transaction.getDescription() + " " + line.replaceAll("\\s+", ""));
			}
			
		}
		return transactions;
	}
    
    private List<Transaction> extractTransactionsKOTAK_6(String pdfText, String accountNo)
			throws IOException, InterruptedException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile("(^\\s*\\d*\\s*\\d{2}/\\d{2}/\\d{4}[\\s\\S]*?)(?=^.*\\d{2}/\\d{2}/\\d{4})",
				Pattern.MULTILINE);
		Pattern pattern2 = Pattern.compile("^\\s*Sl.No.\\s*Date.*\n", Pattern.MULTILINE);
		Matcher matcher2 = pattern2.matcher(pdfText);
		pdfText = matcher2.replaceAll("");

		Matcher matcher = pattern.matcher(pdfText);
		int serialNumCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();

			String txnDate = "", credit = "", debit = "";
			String description = "", balance = "";
			String[] lines = matcher.group(1).split("\\n");
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].replaceFirst("^\\s+", "");

				if (i == 0 && line.length() > 128) {
					txnDate = line.substring(7, 29).trim();
					description = line.substring(29, 65).trim();
					debit = line.substring(96, 130).trim();
					balance = line.substring(130).trim();
					balance = balance.replaceAll("\\s+|C|R", "").trim();
				} else if (i > 0) {
					description += " " + line.trim();
				}
			}
			if (debit.contains("D")) {
				transaction.setTxnType("DEBIT");
				debit = debit.replaceAll("\\s+|D|R", "").trim();
				transaction.setAmount(debit);
				transaction.setCredit("");
				transaction.setDebit(debit);
			} else {
				transaction.setTxnType("CREDIT");
				credit = debit.replaceAll("\\s+|C|R", "").trim();
				transaction.setAmount(credit);
				transaction.setCredit(credit);
				transaction.setDebit("");
			}
			transaction.setsNo(Integer.toString(serialNumCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd/MM/yyyy"));
			transaction.setDescription(description.replaceAll("\\s+", " ").trim());
			transaction.setAccNo(accountNo);
			transaction.setBalance(balance);
			transactions.add(transaction);
		}
		return transactions;
	}
}