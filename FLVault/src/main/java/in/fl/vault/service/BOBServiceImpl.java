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
public class BOBServiceImpl implements BOBService{
	private static final Logger log = Logger.getLogger(StmtServiceImpl.class);
	
	@Override
	public BSInfo parseBOB1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOBServiceImpl parseBOB1 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		List<Transaction> transactions = new ArrayList<>();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			Pattern txnPattern = Pattern.compile("(.*)\\n{1}(\\s*\\d{1,}\\s*\\d{2}-\\d{2}-\\d{4}.*\\n)");
			Matcher txnMatcher = txnPattern.matcher(pdfText);
			
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number.*\\n\\s+(\\w+)").trim();
			bsInfo.setAccountNo(accountNo);
			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type.*\\n\\s+(\\w+)").trim());
			bsInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "Account\\s*Name.*\\n([\\s\\S]*?)\\n.*Account\\s*Number", 70));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Customer\\s*Address.*\\n([\\s\\S]*?)\\n\\s*S", 90));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC[\\s\\S]*?(BARB\\w{7})"));
			bsInfo.setBranch(CommonUtils.extractMultiLinesField(pdfText, "Branch\\s*Name.*\\n([\\s\\S]*?)\\n.*IFSC", 90, 190));
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Account\\s*Statement.*?(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if(period != null) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1].trim(), dateFormat));
			}

			List<String> regions = new ArrayList<>();
			if(txnMatcher.find()) {}
			while (txnMatcher.find()) {
				regions.add(txnMatcher.group());
			}
			int serialNoCount = 1;
			for (String region : regions) {
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				String valueDate="";
				Transaction transaction = new Transaction();
				
				String []listlist=region.split("\\n");
				for(int i=0;i<=listlist.length-1;i++){
	        	   String line=listlist[i];
	        	   if(i==1){
	        		   if(line.length()<171){
	        			   log.error("Region length is insufficient for full extraction."+ region);
	        			   continue;
	        		   }
	        		   date = line.substring(15, 31).trim();
		           	   valueDate=line.substring(32,47).trim();
		           	   description+= line.substring(47, 106).trim();
		           	   debit+=line.substring(128,149).trim();
		           	   credit+= line.substring(149, 171).trim();
		           	   balance+= line.substring(171).trim();  
		    	   }else {
		    		  description+= line.trim();
		    	   }
				}
		        if(credit.length()>0 && !credit.equalsIgnoreCase("-")){
		    	    transaction.setTxnType("CREDIT");
		    	    transaction.setAmount(credit);
		        } else {
		        	transaction.setAmount(debit);
		    	    transaction.setTxnType("DEBIT");
		        }
		        transaction.setCredit(credit.equalsIgnoreCase("-") ? "" : credit);
				transaction.setDebit(debit.equalsIgnoreCase("-") ? "" : debit);
	         	transaction.setsNo(String.valueOf(serialNoCount++));
	         	String txnDateFormat = "dd-MM-yyyy";
	            transaction.setValueDate(CommonUtils.dateFormatter(valueDate, txnDateFormat));
				transaction.setTxnDate(CommonUtils.dateFormatter(date, txnDateFormat));
				transaction.setBalance(balance);
				transaction.setAccNo(accountNo);
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOBServiceImpl parseBOB1:", e);
		}
		log.info("Exiting BOBServiceImpl parseBOB1: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOBServiceImpl parseBOB1 is ==>" + timeTaken);
		return bsInfo;
	}
	
	@Override
	public BSInfo parseBOB2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOBServiceImpl parseBOB2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		List<Transaction> transactions = new ArrayList<>();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "4");
			Pattern txnPattern = Pattern
						.compile("^(\\s*?\\d{2}/\\d{2}/\\d{4}[\\s\\S]*?)(?=\\s*Date|\\s*\\d{2}/\\d{2}/\\d{4})", Pattern.MULTILINE);
			Matcher txnMatcher = txnPattern.matcher(pdfText);

			bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(.*)").trim()); 
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(BARB\\w{7})"));  
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Period\\s*from\\s*(\\d{2}/\\d{2}/\\d{4})\\s*to\\s*(\\d{2}/\\d{2}/\\d{4})");
			String dateFormat = "dd/MM/yyyy";
			if(period != null && period.length==2){
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1].trim(), dateFormat));
			}
            
			String name="";
			for(int i=0;i<pdfText.length();i++){
				char ch=pdfText.charAt(i);
				if(ch=='\n'){
					break;
				}
				name+=ch;
			}
			bsInfo.setName(name.replaceAll("\\s+", " ").trim());
			
			int position=pdfText.indexOf("Customer");
			String address=pdfText.substring(0, position);
			address=address.replaceAll(name, "").replaceAll("\\n", "").replaceAll("\\s+", " ");
		    bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			
			List<String> regions = new ArrayList<>();
			while (txnMatcher.find()) {
				regions.add(txnMatcher.group());
			}
			int serialNoCount = 1;
			for (String region : regions) {
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				String valueDate="";
				Transaction transaction = new Transaction();
				
				String[] listlist=region.split("\\n");
			
				for(int i=0;i<=listlist.length-1;i++){
	        	   String line=listlist[i];
	        	   int len=line.length();
	        	   if(len>0){
	        		   if(len>20)
	        			   date+= line.substring(0, 20).trim();
	        		   else 
	        			   date+= line.substring(0).trim();
	        	   }
	        	   if(len>20){
	        		   if(len>84)
	        			   description+= line.substring(20, 84).trim();
	        		   else
	        			   description+= line.substring(20).trim();
	        	   }
	        	   if(len>84){
	        		   if(len>141)
	        			   debit+= line.substring(84,141).trim();
	        		   else 
	        			   debit+= line.substring(84).trim();
	        	   }
	        	   if(len>141){
	        		   if(len>169)
	        			   credit+= line.substring(141,169).trim();
	        		   else 
	        			   credit+= line.substring(141).trim();
	        	   }
	        	   if(len>169){
		           	   balance+= line.substring(169).trim();
	        	   }
				}
		       	if(credit.length()>0 && !credit.equalsIgnoreCase("-")){
		       		transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				} else {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				}
	         	transaction.setsNo(String.valueOf(serialNoCount++));
	         	String txnDateFormat = "dd/MM/yyyy";
	         	balance = balance.replaceAll("C|r", "");
	         	transaction.setTxnDate(CommonUtils.dateFormatter(date, txnDateFormat));
	         	transaction.setValueDate(CommonUtils.dateFormatter(valueDate, txnDateFormat));
				transaction.setBalance(balance);
				transaction.setCredit(credit);
				transaction.setDebit(debit);
				transaction.setAccNo(bsInfo.getAccountNo());
				transaction.setDescription(description.replaceAll("\\s+"," ").trim());
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOBServiceImpl parseBOB2:", e);
		}
		log.info("Exiting BOBServiceImpl parseBOB2: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOBServiceImpl parseBOB2 is ==>" + timeTaken);
		return bsInfo;
	}
	
	@Override
	public BSInfo parseBOB3(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOBServiceImpl parseBOB3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC[\\s\\S]*?(BARB\\w{7})"));
			String[] accountArray = CommonUtils.extractMultiGroupArray(pdfText,
					"Statement\\s*of\\s*transactions\\s*in\\s*(.*?)(\\d+)");
			String addressRegion = CommonUtils.extractField(pdfText,
					"([\\s\\S]*?)(?=\\s*Your\\s*Account\\s*Statement)");
			String[] addressLine = addressRegion.split("\\n");
			String name = "";
			String address = "";
			boolean isAddress = false;
			if (addressLine != null) {
				for (String line : addressLine) {
					String originalLine = (line.length() > 85 ? line.substring(0, 85) : line);
					if (originalLine.trim().isEmpty())
						isAddress = true;

					if (isAddress) {
						address += originalLine;
					} else {
						name += originalLine;
					}
				}
				address = address.replaceAll("\\s+", " ").trim();
				name = name.replaceAll("\\s+", " ");
				bankStatementInfo.setAddress(address);
				bankStatementInfo.setName(name);
			}
			if (accountArray.length == 2) {
				bankStatementInfo.setAccountType(accountArray[0].trim());
				bankStatementInfo.setAccountNo(accountArray[1].trim());
			}
			String dateFormat = "MMMddyyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"Statement\\s*Period.*?(\\w{3}\\s*\\d{2}\\s*,\\s*\\d{4})\\s*to\\s*(\\w{3}\\s*\\d{2}\\s*,\\s*\\d{4})");
			if (period != null && period.length >= 2) {
				String startdate = period[0].replaceAll(",|\\s*", "");
				String enddate = period[1].replaceAll(",|\\s*", "");
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startdate, dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(enddate, dateFormat));
			}
			bankStatementInfo.setTransactions(extractTransactionsBOB_3(pdfText, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace()
			log.error("Error in BOBServiceImpl parseBOB3: ", e);
		}
		log.info("Exiting BOBServiceImpl parseBOB3: " + request);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOBServiceImpl parseBOB3 is ==> " + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseBOB4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOBServiceImpl parseBOB4 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		List<Transaction> transactions = new ArrayList<>();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "5");
			Pattern txnPattern = Pattern.compile(
					"^(\\s*?\\d{2}/\\d{2}/\\d{4}\\s*?\\d{2}/\\d{2}/\\d{4}[\\s\\S]*?)(?=\\s*Date|\\s*\\d{2}/\\d{2}/\\d{4})",
					Pattern.MULTILINE);
			Matcher txnMatcher = txnPattern.matcher(pdfText);

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"Statement\\s*Period\\s*from\\s*(\\d{2}/\\d{2}/\\d{4})\\s*to\\s*(\\d{2}/\\d{2}/\\d{4})");
			String dateFormat = "dd/MM/yyyy";
			if (period != null && period.length == 2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1].trim(), dateFormat));
			}
			bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(.*)").trim());
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*:\\s*(.*)MICR").replaceAll("\\s+", " ").trim());
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(\\w*)"));

			Pattern addressPatter = Pattern.compile("Address\\s*:([\\s\\S]*?)(?=\\s*Account\\s*No)", Pattern.MULTILINE);
			Matcher addressMatcher = addressPatter.matcher(pdfText);
			
			bsInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Holder\\s*Name\\s*:(.*)Add").replaceAll("\\s+", " ").trim());

			String address = "";
			if (addressMatcher.find()) {
				String addressLine[] = addressMatcher.group(1).split("\\n");
				if (addressLine.length > 0) {
					address += addressLine[0];
				}
				for (int i = 1; i < addressLine.length; i++) {
					if (addressLine[i].length() > 75) {
						address += " " + addressLine[i].substring(75);
					}
				}
			}
			bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());

			List<String> regions = new ArrayList<>();
			while (txnMatcher.find()) {
				regions.add(txnMatcher.group());
			}
			int serialNoCount = 1;
			for (String region : regions) {
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				String valueDate = "";
				Transaction transaction = new Transaction();

				String[] listlist = region.split("\\n");

				for (int i = 0; i <= listlist.length - 1; i++) {
					String line = listlist[i];
					int len = line.length();
					if (len > 0) {
						if (len > 15)
							date += line.substring(0, 15).trim();
						else
							date += line.substring(0).trim();
					}
					if (len > 15) {
						if (len > 30)
							valueDate += line.substring(15, 30).trim();
						else
							valueDate += line.substring(15).trim();
					}
					if (len > 30) {
						if (len > 75)
							description += line.substring(30, 75).trim();
						else
							description += line.substring(30).trim();
					}
					if (len > 93) {
						if (len > 116)
							debit += line.substring(93, 116).trim();
						else
							debit += line.substring(93).trim();
					}
					if (len > 116) {
						if (len > 137)
							credit += line.substring(116, 137).trim();
						else
							credit += line.substring(116).trim();
					}
					if (len > 137) {
						balance += line.substring(137).trim();
					}
				}
				if (credit.length() > 0 && !credit.equalsIgnoreCase("-")) {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				} else {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				}
				balance = balance.replaceAll("C", "").replaceAll("r", "");

				transaction.setsNo(String.valueOf(serialNoCount++));
				String txnDateFormat = "dd/MM/yyyy";
				transaction.setTxnDate(CommonUtils.dateFormatter(date, txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(valueDate, txnDateFormat));
				transaction.setBalance(balance);
				transaction.setCredit(credit);
				transaction.setDebit(debit);
				transaction.setAccNo(bsInfo.getAccountNo());
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOBServiceImpl parseBOB4:", e);
		}
		log.info("Exiting BOBServiceImpl parseBOB4: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOBServiceImpl parseBOB4 is ==>" + timeTaken);
		return bsInfo;
	}
	
	private List<Transaction> extractTransactionsBOB_3(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		try {
			Pattern removePattern = Pattern.compile("^\\s*Page[\\s\\S]*?BALANCE", Pattern.MULTILINE);
			Matcher removeMatcher = removePattern.matcher(pdfText);
			pdfText = removeMatcher.replaceAll("");
			Pattern pattern = Pattern.compile(
					"(^\\s{0,14}\\d{2}-\\d{2}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\d{2}-\\d{4}|^\\s*Page|^\\s*\\d{2}-\\d{2}-\\d{4}\\s*ClosingBalance)",
					Pattern.MULTILINE);
			int serialNoCount = 1;
			Matcher matcher = pattern.matcher(pdfText);
			String firstLine = "";

			int i = 0;
			while (matcher.find()) {
				Transaction transaction = new Transaction();
				String description = "";
				String credit = "";
				String debit = "";
				String txnDate = "";
				String balance = "";
				String[] lines = matcher.group(1).split("\\n");
				if (i == 0) {
					i++;
					if (lines.length <= 1)
						continue;
					firstLine = lines[1].trim();
					continue;
				}
				description = firstLine;
				boolean isMultiLine = false;
				if (lines != null && lines[0].trim().isEmpty()) {
					lines = Arrays.copyOfRange(lines, 1, lines.length);
				}
				int len = lines.length;

				if (len > 0) {
					lines[0] = lines[0].replaceFirst("\\s+", "");
					if (lines[0].length() > 115) {
						txnDate = lines[0].substring(0, 11).trim();
						description += lines[0].substring(11, 66).trim();
						if (lines[0].substring(11, 66).trim().equalsIgnoreCase("ClosingBalance")) {
                            break;
                        }
						credit = lines[0].substring(94, 116).trim();
						debit = lines[0].substring(72, 94).trim();
						balance = lines[0].substring(116).trim();
						if (lines[0].substring(12, 55).trim().isEmpty()) {
							isMultiLine = true;
						}
					}
				}
				if (len >= 2) {
					if (isMultiLine) {
						description += lines[1].trim();
						if (len >= 3) {
							firstLine = lines[2].trim();
						} else {
							firstLine = "";
						}
					} else {
						firstLine = lines[1].trim();
					}
				}

				if (balance.contains("Cr")) {
					balance = balance.replaceAll("Cr", "").trim();
				}
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd-MM-yyyy"));
				transaction.setDescription(description.replaceAll("\\s+", " "));
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				transaction.setBalance(balance);

				if (debit.equalsIgnoreCase("")) {
					transaction.setTxnType("CREDIT");
					transaction.setAmount(credit);
				} else {
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
					transaction.setAmount(debit);
				}
				transactions.add(transaction);
			}
			transactions.remove(transactions.size() - 1);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOBServiceImpl extractTransactionsBOB_3: ", e);
		}
		return transactions;
	}

}