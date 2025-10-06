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
public class BOIServiceImpl implements BOIService {

	private final static Logger log = Logger.getLogger(BOIServiceImpl.class);

	@Override
	public BSInfo parseBOI1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOIServiceImpl parseBOI1 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		List<Transaction> transactions = new ArrayList<>();
		try {

			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			Pattern patternTxn = Pattern.compile("(^\\s*\\d{2}-\\w*-\\d{4}[\\s\\S]*?)(?=^\\s*Contents|^\\s*\\d{2}-\\w*-\\d{4}|^\\s*Grand\\s*Total)", Pattern.MULTILINE);
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:(.*)"));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\s*Code\\s*:(.*)"));
			bsInfo.setEmail(CommonUtils.extractField(pdfText, "Customer\s*e-mail\\s*:(.*)"));
			String accountRegion = CommonUtils.extractField(pdfText, "(Account\\s*Type\\s*[\\s\\S]*?INR)");
			int position = pdfText.indexOf("Statement");
			String addressRegion = pdfText.substring(0, position);
			String addressLine[] = addressRegion.split("\\n");
			String address = "";
			int cnt = 0;
			for (String line : addressLine) {
				if (cnt == 0) {
					String name = line.length() > 70 ? line.substring(0, 70) : line;
					bsInfo.setName(name.replaceAll("\\s+", " ").trim());
					cnt++;
					continue;
				}
				if (line.length() > 70) {
					address += line.substring(0, 70) + " ";
				} else {
					address += line.substring(0) + " ";
				}
			}
			address = address.replaceAll("\\s+", " ");
			bsInfo.setAddress(address.trim());

			String accountLine[] = accountRegion.split("\\n");
			int length = accountLine.length;
			if (length > 1) {
				bsInfo.setAccountType(accountLine[length - 1].substring(0, 50).replaceAll("\\s+", " ").trim());
				bsInfo.setAccountNo(accountLine[length - 1].substring(50, 110).trim());
			}

			List<String> regions = new ArrayList<>();
			Matcher matcher = patternTxn.matcher(pdfText);
			while (matcher.find()) {
				regions.add(matcher.group());
			}
			int serialNoCount = 1;
			for (int i = 0; i < regions.size(); i++) {
				Transaction transaction = new Transaction();
				String description = "";
				String credit = "";
				String debit = "";
				String txnDate = "";
				String balance = "";
				String[] lines = regions.get(i).split("\\n");

				String txnDateFormat = "dd-MMM-yyyy";

				if (i == 0) {
//					System.out.println(lines[0].substring(0,24));
					bsInfo.setStartDate(CommonUtils.dateFormatter(lines[0].substring(0, 24).trim(), txnDateFormat));
				}
				if (i == regions.size() - 1) {
//					System.out.println(lines[0].substring(0,24));
					bsInfo.setEnDate(CommonUtils.dateFormatter(lines[0].substring(0, 24).trim(), txnDateFormat));
				}

				for (int j = 1; j <= lines.length; j++) {
					if (j == 1) {
						txnDate = lines[0].substring(0, 24).trim();
						description += lines[0].substring(24, 83).trim();
						debit = lines[0].substring(115, 142).trim();
						credit = lines[0].substring(142, 169).trim();
						balance = lines[0].substring(170).trim();
					} else {
						description += lines[j - 1].trim();
					}
				}
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(bsInfo.getAccountNo());
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, txnDateFormat));
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
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOIServiceImpl parseBOI1: " + e);
		}

		log.info("Exiting BOIServiceImpl parseBOI1:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOIServiceImpl parseBOI1 is: " + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseBOI2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOIServiceImpl parseBOI2 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankstatementInfo.setName(CommonUtils.extractField(text, "Bank\\s*Of\\s*India.*\\n(.*)\\s*CUSTID").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "A\\/C\\s*NO\\s*:\\s*(\\S*)"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*CODE\\s*:\\s*(\\S*)"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "NOMINEE\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "CUSTID.*\\n([\\s\\S]*?)\\n\\s*JOINT\\s*HOLDER", 0, 80));
			bankstatementInfo.setAccountType(CommonUtils.extractField(text, "TYPE\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());

			String dateFormat = "dd-MM-yyyy";
			bankstatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Account\\s*\\S*\\s*FROM\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankstatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Account\\s*\\S*\\s*.*?TO\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			List<Transaction> transactions = extractTransactionsBOI_2(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOIServiceImpl parseBOI2: " + e);
		}

		log.info("Exiting BOIServiceImpl parseBOI2: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOIServiceImpl parseBOI2 is: " + timeTaken);
		return bankstatementInfo;
	}

	@Override
	public BSInfo parseBOI3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering BOIServiceImpl parseBOI3 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "8");
			bankstatementInfo.setName(CommonUtils.extractField(text, "holder\\s*name\\s*:(.*?)\\s*Account").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*number\\s*:\\s*(\\S*)"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S*)"));
			bankstatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)\\n").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Date\\s*:.*?\\n([\\s\\S]+?)\\n\\s*Customer\\s*ID", 129, 170));

			String dateFormat = "dd-MM-yyyy";
			bankstatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Transaction\\s*Date\\s*from\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankstatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Transaction\\s*Date.*?to\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));

			List<Transaction> transactions = extractTransactionsBOI_3(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in BOIServiceImpl parseBOI3: " + e);
		}

		log.info("Exiting BOIServiceImpl parseBOI3: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for BOIServiceImpl parseBOI3 is: " + timeTaken);
		return bankstatementInfo;
	}

	private List<Transaction> extractTransactionsBOI_2(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row);
				line = line.replaceAll("\\r", " ");
				line = line.replaceAll("\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				// first column is Txn Date, second column is value
				String[] data = line.split("\\|");
				if (data.length == 0 || data.length != 7 || data[0].equalsIgnoreCase("SNO"))
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[3]);

				if (data[4] != null && !data[4].equalsIgnoreCase("") && !data[4].equals("-") && !data[4].equals("0.00")) {
					transaction.setAmount(data[4]);
					transaction.setDebit(data[4]);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(data[5]);
					transaction.setCredit(data[5]);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
				}
				transaction.setBalance(data[6].replaceAll("Cr", ""));
				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsBOI_3(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row);
				line = line.replaceAll("\\r", " ");
				line = line.replaceAll("\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				// first column is Txn Date, second column is value
				String[] data = line.split("\\|");
				if (data.length == 0 || data.length != 6 || data[1].equalsIgnoreCase("Date"))
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[2]);

				if (data[3] != null && !data[3].equalsIgnoreCase("") && !data[3].equals("-") && !data[3].equals("0.00")) {
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
				transaction.setBalance(data[5].replaceAll("₹\s*", ""));
				transactions.add(transaction);
			}
		}
		return transactions;
	}
}