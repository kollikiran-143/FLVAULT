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
public class CUBServiceImpl implements CUBService{
	
	private final static Logger log = Logger.getLogger(CNTBServiceImpl.class);
	
	public BSInfo parseCUB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CUBServiceImpl parseCUB1 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			
			String accountNo = CommonUtils.extractField(text, "Account\\s*Number\\s*:(.*)");
			bankStatementInfo.setAccountNo(accountNo);
			String[] startDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?(\\d{4})");
			String startdate = "";
			if(startdate!=null && startDate.length>=2) {
				startdate += startDate[0]+" "+startDate[1];
				startdate = startdate.trim();
			}
			String[] endDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*To\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?\\d{4}\\s*(\\d{4})");
			String enddate = "";
			if(enddate!=null && endDate.length>=2) {
				enddate += endDate[0]+" "+endDate[1];
				enddate = enddate.trim();
			}
			String dateFormat="dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startdate, dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(enddate, dateFormat));
			
			bankStatementInfo = extractTransactionsCUB_1(filepath, bankStatementInfo, accountNo);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in CUBServiceImpl parseCUB1: "+e);
        }
        log.info("Exiting CUBServiceImpl parseCUB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CUBServiceImpl parseCUB1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	public BSInfo parseCUB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CUBServiceImpl parseCUB2 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "7");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "ACCOUNT\\s*NO\\(.*?:\\s*(\\d*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:(.*)"));
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "ACCOUNT\\s*TYPE\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo
					.setName(CommonUtils.extractField(text, "CUSTOMER\\s*DETAILS\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*:\\s*(.*)"));
			String[] period = CommonUtils.extractMultiGroupArray(text,
					"STATEMENT\\s*OF\\s*ACCOUNT\\s*from\\s*(.*)\\s*to\\s*(.*)");
			String dateFormat = "dd/MM/yyyy";
			
			if(period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "DETAILS\\s*:\\s*([\\s\\S]*?)\\s*(?=Statement)").replaceAll("\\s+"," "));
			bankStatementInfo.setTransactions(extractTransactionsCUB_2(filepath, bankStatementInfo.getAccountNo(), dateFormat));
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in CUBServiceImpl parseCUB2: "+e);
        }
        log.info("Exiting CUBServiceImpl parseCUB2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CUBServiceImpl parseCUB2 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	public BSInfo parseCUB3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CUBServiceImpl parseCUB3 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "5");
			List<Transaction>transactions=new ArrayList<>();
			Pattern patternTxn = Pattern.compile(
                    "(^\\s*\\d{2}-\\w{3}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\w{3}-\\d{4}|\\s*Page|\\s*Total|^\\s*TO\\s*ONL)",
                    Pattern.MULTILINE);
			Pattern namePattern=Pattern.compile("Customer\\s*No.*([\\s\\S]*?)(?=\\s*Opening\\s*Balance)");
			Pattern datePattern=Pattern.compile("Statement\\s*Dt\\s*:\\s*(\\d{2}-\\w*-\\d{4})\\s*to\\s*(\\d{2}-\\w*-\\d{4})");
			Matcher matcher=patternTxn.matcher(pdfText);
			Matcher nameMatcher=namePattern.matcher(pdfText);
			Matcher dateMatcher=datePattern.matcher(pdfText);
			
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\w*)");
			bsInfo.setAccountNo(accountNo);
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch:(.*)").replaceAll("\\s+"," ").trim());
			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*([\\s\\S]*?\\s{10,})").replaceAll("\\s+"," ").trim());
			
			String dateFormat = "dd-MMM-yyyy";
		    if(dateMatcher.find()) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(dateMatcher.group(1).trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(dateMatcher.group(2).trim(), dateFormat));
		    }
			if(nameMatcher.find()) {
				String name="";
				String address="";
				String []lines=nameMatcher.group(1).split("\\n");
				for(int i=0;i<lines.length;i++) {
					if(i<2) {
						name+=lines[i].trim();
					} else {
						address+=lines[i].trim();
					}
				}
				name=name.replaceAll("\\s+"," ");
				bsInfo.setName(name.replaceAll("\\s+"," "));
				bsInfo.setAddress(address.replaceAll("\\s+"," "));
			}
			int serialNoCount=1;  
			while (matcher.find()) {
				Transaction transaction=new Transaction();	
				String description="";
				String txnDate="";
				String credit="";
				String debit="";
				String balance="";
				String []lines = matcher.group(1).split("\\n");
				for(int i=0;i<lines.length;i++)
				{
					String line=lines[i];
					txnDate += (line.length() > 15) ? line.substring(0, 15) : line;
					description += (line.length() > 82) ? line.substring(15, 82) : (line.length() > 15) ? line.substring(15) : "";
					debit += (line.length() > 113) ? line.substring(82, 113) : (line.length() > 82) ? line.substring(82) : "";
					credit += (line.length() > 138) ? line.substring(113, 138) : (line.length() > 113) ? line.substring(113) : "";
					balance += (line.length() > 138) ? line.substring(138) : "";
				}
				debit=debit.replaceAll("\\s*", "");
				if(debit.equalsIgnoreCase("")) {
                    transaction.setTxnType("CREDIT");
                    transaction.setAmount(credit.replaceAll("\\s+", " ").trim());
                } else {
                    transaction.setTxnType("DEBIT"); 
                    transaction.setAmount(debit.replaceAll("\\s+", " ").trim());
                }
				description = description.replaceAll("\\s+", " ");
				transaction.setDebit(debit.replaceAll("\\s+", " ").trim());
				transaction.setCredit(credit.replaceAll("\\s+", " ").trim());
				transaction.setBalance(balance.replaceAll("\\s+", " ").trim());
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setDescription(description.trim());
			
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in CUBServiceImpl parseCUB3: "+e);
        }
        log.info("Exiting CUBServiceImpl parseCUB3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CUBServiceImpl parseCUB3 is ==>" + timeTaken);
        return bsInfo;
	}
	
	private BSInfo extractTransactionsCUB_1(String fileName, BSInfo bankStatementInfo, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		int type = 0;
		String dateFormat ="yyyy-MM-dd";
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty() || row.length < 6) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length >= 8) {
					if (data[0].equals("Name")) {
						type = 1;
						continue;
					} else if (data[0].equals("Opening Date")) {
						type = 2;
						continue;
					} else if (data[0].equals("Trxn ID")) {
						type = 3;
						continue;
					}
					if (type == 1) {
						bankStatementInfo.setName(data[0]);
						bankStatementInfo.setDob(CommonUtils.dateFormatter(data[1],dateFormat));
						bankStatementInfo.setPhone1(data[2]);
						bankStatementInfo.setEmail(data[4]);
						bankStatementInfo.setPan(data[5].replaceAll("\\s+", ""));
						bankStatementInfo.setAddress(data[6]);
						bankStatementInfo.setNominee(data[8]);
					}
					if (type == 2) {
						bankStatementInfo.setAccountType(data[1]);
						bankStatementInfo.setBranch(data[2]);
						bankStatementInfo.setIfsc(data[3].replaceAll("\\s+", ""));
					} 
					if (type == 3) {
						Transaction transaction = new Transaction();
						
						transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
						transaction.setDescription(data[4]);
						transaction.setTxnType(data[5]);
						if(data[5].equalsIgnoreCase("DEBIT")) {
							transaction.setDebit(data[6]);
						}else {
							transaction.setCredit(data[6]);
						}
						transaction.setAmount(data[6]);
						transaction.setBalance(data[7]);
						transaction.setAccNo(accountNo);
						
						transaction.setsNo(String.valueOf(serialNoCount++));
						transactions.add(transaction);
					}
				}
			}
		}
		bankStatementInfo.setTransactions(transactions);
		return bankStatementInfo;
	}

	public List<Transaction> extractTransactionsCUB_2(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
		
				if (!line.trim().isEmpty() && row.length > 4) {
					String[] data = line.split("\\|");
					// Skip header row
					if (data.length == 6 && !data[0].equals("DATE") && !data[0].equalsIgnoreCase("TOTAL")) {
						Transaction transaction = new Transaction();
						transaction.setsNo(Integer.toString(serialNoCount++));
						String date = CommonUtils.dateFormatter(data[0], dateFormat);
						transaction.setTxnDate(date); // First column for date
						transaction.setDescription(data[1]); // Second column for description
						transaction.setDebit(data[3].isEmpty() ? "" : data[3].trim()); // Third column for debit
						transaction.setCredit(data[4].isEmpty() ? "" : data[4].trim()); // Fourth column for credit
						// Set debit/credit and amount based on the available field
						if (!transaction.getDebit().isEmpty()) {
							transaction.setAmount(transaction.getDebit());
							transaction.setTxnType("DEBIT");
						} else {
							transaction.setAmount(transaction.getCredit());
							transaction.setTxnType("CREDIT");
						}
						transaction.setBalance(data[5]); // Fifth column for balance
						transaction.setAccNo(accountNo);
						transactions.add(transaction);
					}
				}
			}
		}
		return transactions;
	}

}
	