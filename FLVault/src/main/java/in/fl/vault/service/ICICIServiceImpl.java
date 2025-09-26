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
public class ICICIServiceImpl implements ICICIService{
	
	private static final Logger log = Logger.getLogger(StmtServiceImpl.class);
	
	@Override
	public BSInfo parseICICI1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ICICIServiceImpl parseICICI1 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			Pattern patternBranch = Pattern.compile("(.*)Your\\s*Base\\s*Branch\\s*:(.*)([\\s\\S]*?)(?=Visit)");
			Pattern patternAccount = Pattern
					.compile("ACCOUNT\\s*TYPE\\s*ACCOUNT\\s*NUMBER.*\\n\\s*(.*?)\\s+(.*?)\\s+(.*?)\\s+(.*?)\\s+(.*)");
			Matcher matcherBranch = patternBranch.matcher(pdfText);
			Matcher matcherAccount = patternAccount.matcher(pdfText);

			String[] date = CommonUtils.extractMultiGroupArray(pdfText, "for\\s*the\\s*period\\s*(.*?)-(.*)");
			String addressRegion = CommonUtils.extractField(pdfText,
					"(.*Your\\s*Base\\s*Branch\\s*:[\\s\\S]*?)(?=\s*Did\\s*you\\s*know)");

			String dateFormat = "MMMM dd, yyyy";
			if (date.length == 2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(date[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(date[1].trim(), dateFormat));
			}
			String address = "";
			String addressline[] = addressRegion.split("\\n");
			for (int i = 2; i < addressline.length; i++) {
				if (addressline[i].length() > 70) {
					address += " " + addressline[i].substring(0, 70).trim();
				} else {
					address += " " + addressline[i].trim();
				}
			}
			bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			bsInfo.setName(
					CommonUtils.extractField(pdfText, "(.*)\\n?\\s*Your\\s*Base").replaceAll("\\s+", " ").trim());
			String branch = "";
			if (matcherBranch.find()) {
				String branchlines[] = matcherBranch.group(3).split("\\n");
				branch += matcherBranch.group(2).trim() + " ";
				for (int i = 0; i < branchlines.length; i++) {
					if (branchlines[i].length() > 70) {
						branch += " " + branchlines[i].substring(70).trim();
					}
				}
			}
			bsInfo.setBranch(branch.replaceAll("\\s+", " "));
			if (matcherAccount.find()) {
				bsInfo.setAccountType(matcherAccount.group(1).trim().replaceAll("\\s+", " "));
				bsInfo.setAccountNo(matcherAccount.group(2).trim());
				bsInfo.setIfsc(matcherAccount.group(4).trim());
				bsInfo.setNominee(matcherAccount.group(5).trim());
			}
			List<Transaction> transactions = extractTransactionsICICI_1_4(pdfText, bsInfo.getAccountNo(), "dd-MM-yyyy");
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in ICICIServiceImpl parseICICI1: " + e);
		}
		log.info("Exiting ICICIServiceImpl parseICICI1:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for ICICIServiceImpl parseICICI1 is ==>" + timeTaken);
		return bsInfo;
	}
	
	@Override
	public BSInfo parseICICI2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ICICIServiceImpl parseICICI2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			bsInfo.setName(CommonUtils.extractField(pdfText, "Details\\s*With\\s*Us.*\\n(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Details\\s*With\\s*Us.*\\n([\\s\\S]*?)\\n.*Base\\s*Branch", 100));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC[\\s\\S]*?(ICIC\\w{7})"));
			
			String dateFormat = "dd-MM-yyyy";
			String[] accNoPeriod = CommonUtils.extractMultiGroupArray(pdfText,
					"account\\s*number\\s*:\\s*(\\d*).*period\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
			if (accNoPeriod != null) {
				bsInfo.setAccountNo(accNoPeriod[0]);
				bsInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2].trim(), dateFormat));
			}
			bsInfo.setTransactions(extractTransactionsICICI_2(filepath, bsInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//				e.printStackTrace();
			log.error("Error in ICICIServiceImpl parseICICI2: " + e);
		}
		
		log.info("Exiting ICICIServiceImpl parseICICI2:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for ICICIServiceImpl parseICICI2 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseICICI3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ICICIServiceImpl parseICICI3 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "A/C\\s*Type:(.*)").replaceAll("\\s+", " "));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(.*)").trim());
			bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "A/C\\s*No\\s*:\\s*(.*?)\\s{10,}").trim());
			bsInfo.setName(CommonUtils.extractField(pdfText, "Name\\s*:\\s*(.*)A/C").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText,
					"A/C\\s*\\S*:.*([\\s\\S*]*?)\\n\\s*A/C\\s*No\\s*:", 98));
			String txndateFormat = "dd-MMM-yyyy";
			Pattern pattern = Pattern.compile("Period\\s*:\\s*From.*?To\\s*\\d{2}\\/\\d{2}\\/\\d{4}");
			Matcher matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				txndateFormat = "dd/MMM/yyyy";
			}
			List<Transaction> listOfTransactions = extractTransactionsICICI_3(filepath, bsInfo.getAccountNo(),
					txndateFormat);
			bsInfo.setTransactions(listOfTransactions);
			bsInfo.setStartDate(listOfTransactions.get(0).getTxnDate());
			bsInfo.setEnDate(listOfTransactions.get(listOfTransactions.size() - 1).getTxnDate());
		} catch (Exception e) {
//				e.printStackTrace();
			log.error("Error in ICICIServiceImpl parseICICI3: " + e);
		}

		log.info("Exiting ICICIServiceImpl parseICICI3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for ICICIServiceImpl parseICICI3 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseICICI4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ICICIServiceImpl parseICICI4 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			Pattern addressEndPattern = Pattern.compile("Base\\s*Branch");
			
			String [] lines = pdfText.split("\\n");
			int start = 0;
			String address = "";
			for (String eachline : lines) {
				if(start == 0) { start++; continue; }
				if(start == 1) { bsInfo.setName(eachline.replaceAll("\\s+", " ").trim()); start++; continue; }
				Matcher addEndMatcher = addressEndPattern.matcher(eachline);
				if(!addEndMatcher.find()) {
					address += eachline + " ";
				}else {
					break;
				}
			}
			bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*:(.*)").replaceAll("\\s+", " ").trim());
			String[] accTypeNoIfscArr = CommonUtils.extractMultiGroupArray(pdfText, "ACCOUNT\\s*TYPE\\s*ACCOUNT\\s*NUMBER.*IFS\\s*CODE.*\\n\\s*(\\S*)\\s*(\\S*).*(ICIC\\w{7})");
			if(accTypeNoIfscArr != null) {
				bsInfo.setAccountType(accTypeNoIfscArr[0]);
				bsInfo.setAccountNo(accTypeNoIfscArr[1]);
				bsInfo.setIfsc(accTypeNoIfscArr[2]);
			}
			
			String dateFormat = "dd-MM-yyyy";
			List<Transaction> transactions = extractTransactionsICICI_1_4(pdfText, bsInfo.getAccountNo(), dateFormat);
			bsInfo.setStartDate(transactions.get(0).getTxnDate());
			bsInfo.setEnDate(transactions.get(transactions.size()-1).getTxnDate());
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in ICICIServiceImpl parseICICI4: " + e);
		}
		
		log.info("Exiting ICICIServiceImpl parseICICI4:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for ICICIServiceImpl parseICICI4 is ==>" + timeTaken);
		return bsInfo;
	}

	public boolean matchesDatePattern(String[] dateStr) {
		String datePattern = "\\d{2}/\\d{2}/\\d{4}\\.*";
		String noPattern = "\\s*\\d{1,}\\s*";
		Pattern pattern1 = Pattern.compile(datePattern);
		Pattern pattern2 = Pattern.compile(noPattern);
		Matcher matcher1 = pattern1.matcher(dateStr[1]);
		Matcher matcher2 = pattern2.matcher(dateStr[0]);
		return matcher1.find() && matcher2.find();
	}

	public boolean matchesDatePattern2(String[] dateStr) {
		String datePattern = "\\d{2}-\\w*?-\\d{4}\\.*";
		String noPattern = "\\s*\\d{1,}\\s*";
		Pattern pattern1 = Pattern.compile(datePattern);
		Pattern pattern2 = Pattern.compile(noPattern);
		Matcher matcher1 = pattern1.matcher(dateStr[3]);
		Matcher matcher2 = pattern2.matcher(dateStr[0]);
		return matcher1.find() && matcher2.find();
	}
	
	private List<Transaction> extractTransactionsICICI_1_4(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*Page[\\s\\S]*?BALANCE", "");

        Pattern patternremove = Pattern.compile("(^\\s{20,}TOTAL[\\s\\S]?BALANCE\\n)(?=^\\s*\\d{2}-\\d{2}-\\d{4})",
                Pattern.MULTILINE);
        Matcher matcherremove = patternremove.matcher(pdfText);
        pdfText = matcherremove.replaceAll("");
		
		Pattern patternTxn = Pattern.compile(
				"(^\\s*\\d{2}-\\d{2}-\\d{4}.*[\\s\\S]*?)(?=^\\s*TOTAL|^\\s*Total|\\s*\\d{2}-\\d{2}-\\d{4})",
				Pattern.MULTILINE);
		Matcher matcher = patternTxn.matcher(pdfText);
		String firstLine = "";

		int serialCount = 1;
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
//				System.out.println(lines[0]);
				if(lines.length==1)
				{
					continue;
				}
				firstLine = lines[1].trim();
				continue;
			}
			description = firstLine;
			if(lines[0].isEmpty())
			{
				lines = Arrays.copyOfRange(lines, 1, lines.length);
			}
			for (int j = 0; j < lines.length; j++) {
				if (j == 0) {
					if (lines[j].length() < 170) {
						continue;
					}
					txnDate = lines[0].substring(0, 24).trim();
					description += lines[0].substring(32, 109).trim();
					credit = lines[0].substring(115, 145).trim();
					debit = lines[0].substring(145, 170).trim();
					balance = lines[0].substring(170).trim();
				} else if (j != lines.length - 1) {
					description += lines[j].trim();
				} else {
					firstLine = lines[j].trim();
				}
			}
			if (lines.length == 1) {
				firstLine = "";
			} 
			
			if (lines[0].substring(25, 88).trim().equalsIgnoreCase("B/F")) {
                continue;
            }
            balance = balance.replaceAll("[()]", "");
			transaction.setsNo(String.valueOf(serialCount++));
			transaction.setAccNo(accountNo);
			transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
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
		return transactions;
	}

	private List<Transaction> extractTransactionsICICI_2(String filePath, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty() || line.contains("B/F")) {
					continue;
				}
				String[] data = line.split("\\|");
				
				if (data.length == 8 && !data[0].equalsIgnoreCase("Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);

					if (!data[3].isEmpty() && !data[3].equals("-")) {
						transaction.setAmount(data[3]);
						transaction.setDebit(data[3]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[4]);
						transaction.setCredit(data[4]);
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					}
					transaction.setBalance(data[7].replace("Cr", "").trim()); 
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsICICI_3(String filePath, String accountNo, String txnDateFormat)
			throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				int sz = data.length;
				if ((sz == 9 || sz == 10) && !data[sz - 1].equals("Balance")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnId(data[1].replaceAll("\\s+", ""));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[3].replaceAll("\\s+", ""), txnDateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[2].replaceAll("\\s+", ""), txnDateFormat));
					transaction.setDescription(data[sz - 4]);
					transaction.setBalance(data[sz - 1].replaceAll("\\s+", ""));
					String debit = data[sz - 3];
					if (debit == null || debit.equals("") || debit.equals("NA")) { // credit
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
						transaction.setCredit(data[sz - 2].replaceAll("\\s+", ""));
						transaction.setAmount(transaction.getCredit());
					} else {
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
						transaction.setDebit(data[sz - 3].replaceAll("\\s+", ""));
						transaction.setAmount(transaction.getDebit());
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}