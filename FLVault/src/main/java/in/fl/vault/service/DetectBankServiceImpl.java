package in.fl.vault.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.fl.vault.model.FakeStmtsAudit;
import in.fl.vault.repository.FakeStmtsAuditRepository;
import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.utils.CommonUtils;

@Service
public class DetectBankServiceImpl implements DetectBankService {

	@Autowired
	private FakeStmtsAuditRepository fakeStmtsAuditRepository;

	private final Logger log = Logger.getLogger(DetectBankServiceImpl.class);

	@Override
	public String detectBank(String filePath) throws IOException, InterruptedException {

		Pattern pattern = null;
		Matcher matcher = null;

		String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

		if (pdfText == null) {
			throw new FileNotFoundException("Error while parsing PDFs, Provide Valid filepath.");
		}
		String bankCode = extractBankCode(pdfText).toUpperCase();

		switch (bankCode) {
		// Detect SBI
		case "SBIN": {
			pattern = Pattern.compile("IFS\\s*Code\\s*:\\s*SBIN.*\\n\\s*\\(Indian\\s*Financial\\s*System\\)");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_1";
			}
			pattern = Pattern.compile("\\s*IFSC\\s\\s*:\\s*SBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_4";
			}
			pattern = Pattern.compile("\\s*IFS\\s+Code\\s*:\\s*SBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_5";
			}
			pattern = Pattern.compile("\\s*IFSC\\s{2,}Code\\s*:\\s*SBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_6";
			}
			pattern = Pattern.compile("\\s*IFS\\s*Code\\s*SBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("CKYC\\s*No.");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "SBI_8";
				}
				return "SBI_7";
			}
			pattern = Pattern.compile("IFSC\\s+Code\\s+SBIN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_2";
			}
			pattern = Pattern.compile("IFS\\s*\\(Indian\\s*Financial\\s*System\\)\\s*SBIN\\w{7}\\n\\s*Code");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_9";
			}
			break;
		}
		// Detect CANARA Bank
		case "CNRB": {
			pattern = Pattern.compile("IMB\\s*USERS\\s*ARE\\s*REQUESTED\\s*TO \\s*NOTE\\s*THAT\\s*CANARA\\s*BANK\\s*DOES\\s*NOT\\s*SEEK");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CANARA_1";
			}
			pattern = Pattern.compile("\\s*IFSC\\s*Code\\s*CNRB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern2 = Pattern.compile("Statement\\s*for\\s*A\\/c.*\\d{2}-\\w{3}-\\d{4}");
				Pattern pattern3 = Pattern.compile("From\\s*\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}");
				Matcher matcher2 = pattern2.matcher(pdfText);
				Matcher matcher3 = pattern3.matcher(pdfText);
				if (matcher2.find()) {
					return "CANARA_4";
				}
				if (matcher3.find()) {
					return "CANARA_5";
				}
			}
			pattern = Pattern.compile("IFSC\\s{2,}:\\s{2,}CNRB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CANARA_6";
			}
			break;
		}
		// Detect TMB
		case "TMBL": {
			pattern = Pattern.compile("Statement\\s*for\\s*A\\/c");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("(-?\\d*,?\\d*,?\\d+\\.\\d+)\\s+0\\s+(-?\\d*,?\\d*,?\\d+\\.\\d+)");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "TMB_7";
				}
				return "TMB_1";
			}
			pattern = Pattern.compile("Statement\\s*for\\s*account\\s*number");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("\\n\\s*\\d{2}-[A-Za-z]{3}.*\\n.*\\n\\s*\\d{4}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "TMB_6";
				}
				Pattern pattern2 = Pattern.compile("\\n\\s*\\d{2}-[A-Za-z]{3}-.*\\n\\s+\\d{4}\\s+");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher2.find()) {
					return "TMB_8";
				}
				return "TMB_2";
			}
			pattern = Pattern.compile("IFSC\\s{3,}:\\s*TMBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "TMB_3";
			}
			pattern = Pattern.compile("IFSC\\s*CODE\\s*-\\s*TMBL\\w{7}.*\\n\\s*MICR");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "TMB_4";
			}
			pattern = Pattern.compile("IFSC\\s*CODE\\s*-\\s*TMBL\\w{7}.*\\n\\s*Phone");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "TMB_5";
			}
			break;
		}
		// Detect HDFC
		case "HDFC": {
			pattern = Pattern.compile("RTGS\\/NEFT");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("IFSC.*\\n.*Email");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "HDFC_3";
				}
				return "HDFC_2";
			}
			return "HDFC_1";
		}
		// Detect IDFC
		case "IDFB": {
			pattern = Pattern.compile("IFSC\\s*:\\s*IDFB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IDFC_1";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*IDFB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern2 = Pattern.compile("CUSTOMER\\s*ID\\s*:.*\\n\\s*ACCOUNT\\s*NO");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher2.find()) {
					return "IDFC_3";
				}
				return "IDFC_2";
			}
			break;
		}
		// Detect UNION
		case "UBIN": {
			pattern = Pattern.compile("MICR\\s*CODE.*IFSC\\s*UBIN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_4";
			}
			pattern = Pattern.compile("IFSC\\s*UBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_1";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*UBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_2";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*UBIN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_3";
			}
			break;
		}
		// Detect KNB
		case "KARB": {
			Pattern pattern1 = Pattern.compile("Statement\\s*for\\s*A\\/c\\s*\\d*\\s*Between");
			Pattern pattern2 = Pattern.compile("Statement.*for\\s*the\\s*period\\s*:");
			Pattern pattern3 = Pattern.compile("Statement\\s*for\\s*account\\s*number.*Between");
			Matcher matcher1 = pattern1.matcher(pdfText);
			Matcher matcher2 = pattern2.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			if (matcher1.find()) {
				return "KNB_1";
			} else if (matcher2.find()) {
				return "KNB_2";
			} else if (matcher3.find()) {
				pattern = Pattern.compile("^(?!.*\\d{2}-\\d{2}-\\d{4}).*(-?\\d*,?\\d*,?\\d+\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d+\\.\\d+)$");
				matcher = pattern.matcher(pdfText);
				if (matcher.find()) {
					return "KNB_4";
				} else {
					return "KNB_3";
				}
			}
			break;
		}
		// Detect INDALH
		case "IDIB": {
			pattern = Pattern.compile("Branch\\s*IFSC\\s*:\\s*IDIB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDALH_1";
			}
			pattern = Pattern.compile("IFSC\\s*IDIB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDALH_3";
			}
			pattern = Pattern.compile("IFSC\\s*CODE\\s*:\\s*IDIB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDALH_4";
			}
			pattern = Pattern.compile("INDIAN\\s*BANK[\\s\\S]*?IFSC\\s*Code\\s*:\\s*IDIB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDIAN_2";
			}
			break;
		}
		// Detect IOB (Indian Overseas Bank)
		case "IOBA": {
			pattern = Pattern.compile("IFSC\\s*CODE\\s*:\\s*IOBA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IOB_1";
			}
			pattern = Pattern.compile("IFS\\s*Code\\s*:\\s*IOBA|IFS\\s*Code.*?\\n\\s*:\\s*IOBA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IOB_2";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*IOBA\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IOB_3";
			}
			break;
		}
		// Detect BOB
		case "BARB": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*\\n.*BARB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BOB_1";
			}
			pattern = Pattern.compile("IFSC\\s*Code:\\s*BARB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("TRAN\\s*DATE\\s*VALUE\\s*DATE");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "BOB_4";
				}
				return "BOB_2";
			}
			pattern = Pattern.compile("IFSC\\s*CODE:\\s*BARB");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BOB_5";
			}
			pattern = Pattern.compile("IFSC[\\s\\S]*?BARB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find() || pdfText.contains("https://www.bankofbaroda.in")) {
				return "BOB_3";
			}
			break;
		}
		// Detect KOTAK
		case "KKBK": {
			pattern = Pattern.compile("IFSC\\s*KKBK");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Analysis\\s*of\\s*your\\s*account.*\\d{2}\\s*[A-za-z]{3},\\s*\\d{4}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "KOTAK_2";
				} else {
					return "KOTAK_1";
				}
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*KKBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "KOTAK_3";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*KKBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "KOTAK_4";
			}
			break;
		}
		// Detect EQUITAS
		case "ESFB": {
			pattern = Pattern.compile("IFSC\\s*:\\s*ESFB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "EQUITAS_1";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*ESFB\\w{7}\\s*MICR\\s*Code\\s*:");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "EQUITAS_3";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:?\\s*ESFB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern2 = Pattern.compile("Period\\s*from\\s*[A-Za-z]{3}\\s*[A-Za-z]{3}\\s*\\d{2}\\s*\\d{4}");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher2.find()) {
					return "EQUITAS_4";
				}
				return "EQUITAS_2";
			}
			break;
		}
		// Detect UCO
		case "UCBA": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*UCBA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("E-Mail.*UCOBANK.CO.IN");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "UCO_4";
				}
				return "UCO_1";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*UCBA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("UCBA\\w{7}\\s*MICR");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "UCO_3";
				}
				return "UCO_2";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*UCBA\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UCO_5";
			}
			break;
		}
		// Detect STANDARD CHARTERED (STND)
		case "SCBL": {
			pattern = Pattern.compile("IFSC\\s*:\\s*SCBL\\w{7}.*PHONE");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "STND_1";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*SCBL\\w{7}\\s*MICR");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "STND_2";
			}
			pattern = Pattern.compile("BRANCH\\s*ADDRESS[\\s\\S]*?IFSC.*SCBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "STND_3";
			}
			break;
		}
		// Detect South Indian Bank (SIB)
		case "SIBL": {
			pattern = Pattern.compile("IFSC\\s*:\\s*SIBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("PERIOD\\s*(\\d{2}-\\d{2}-\\d{4})\\s*[tToO]");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "SIB_4";
				}
				return "SIB_1";
			}
			pattern = Pattern.compile("IFSC\\s{2,}:\\s{2,}SIBL\\w{7}\\n.*MICR");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SIB_2";
			}
			pattern = Pattern.compile("IFSC\\s:SIBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Nominee:");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "SIB_3";
				}
			}
			break;
		}
		// Detect CSB
		case "CSBK": {
			pattern = Pattern.compile("\\d*\\s*IFSC\\s*CSBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CSB_1";
			}
			pattern = Pattern.compile("Branch\\s*IFSC:\\s*CSBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CSB_2";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*CSBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("From\\s*Date\\s*:\\s*\\d{2}-[A-Za-z]{3}-\\d{4}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				Pattern pattern2 = Pattern.compile("[A-Z]{3}\\s*\\d{1,2},\\d{4}.*INR.*[CD]?r?");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher1.find()) {
					return "CSB_7";
				} else if (matcher2.find()) {
					return "CSB_8";
				}
				return "CSB_4";
			}
			pattern = Pattern.compile("IFS\\s*Code\\s*:\\s*CSBK\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\s+\\d{2}-[A-Za-z]{3}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "CSB_6";
				}
				return "CSB_5";
			}
			break;
		}
		// Detect FEDERAL
		case "FDRL": {
			pattern = Pattern.compile("IFSC\\s*:\\s*FDRL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "FEDERAL_1";
			}
			pattern = Pattern.compile("IFSC\\s*FDRL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "FEDERAL_2";
			}
			break;
		}
		// Detect PNB
		case "PUNB": {
			pattern = Pattern.compile("Statement\\s*of\\s*Account.*For\\s*Period");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "PNB_1";
			}
			pattern = Pattern.compile("Statement\\s*Period\\s*:");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Txn\\s*No\\.\\s*Txn\\s*Date\\s*Desc");
				Matcher matcher1 = pattern1.matcher(pdfText);
				Pattern pattern2 = Pattern.compile("Generated\\s*through\\s*PNB\\s*WA\\s*Banking");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher1.find()) {
					return "PNB_4";
				} else if (matcher2.find()) {
					return "PNB_5";
				} else {
					return "PNB_2";
				}
			}
			pattern = Pattern.compile("Statement\\s*for\\s*Period\\s*:");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "PNB_3";
			}
			break;
		}
		// Detect BANDHAN
		case "BDBL": {
			pattern = Pattern.compile("Statement\\s*period\\s*From\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BANDHAN_1";
			}
			pattern = Pattern.compile("From\\s*Date\\s*:\\s*(\\d{2}-[A-Z]{3}-\\d{4})");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BANDHAN_2";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*BDBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BANDHAN_3";
			}
			break;
		}
		// Detect BOI (Bank Of India)
		case "BKID": {
			pattern = Pattern.compile("IFSC\s*Code:\s*BKID");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BOI_1";
			}
			pattern = Pattern.compile("IFSC\\s*CODE\\s*:\\s*BKID");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BOI_2";
			}
			pattern = Pattern.compile("IFSC\\s*:\\s*BKID");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "BOI_3";
			}
			break;
		}
		// Detect FINCARE
		case "FSFB": {
			pattern = Pattern.compile("IFSC\\s{5,}:\\s*FSFB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "FINCARE_1";
			}
			break;
		}
		// Detect BOM (Bank of Maharashtra)
		case "MAHB": {
			pattern = Pattern.compile("IFSC\\s{5,}MAHB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "MAHB_1";
			}
			break;
		}
		// Detect Punjab & Sind Bank
		case "PSIB": {
			pattern = Pattern.compile("IFSC\\s*:\\s*PSIB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "PSIB_1";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*PSIB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "PSIB_2";
			}
			break;
		}
		// Detect YESB
		case "YESB": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*YESB\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Statement\\s*Type\\s*:\\s*\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "YESB_3";
				}
				return "YESB_1";
			}
		}
		// Detect AXIS
		case "UTIB": {
			pattern = Pattern.compile("Tran\\s*Date\\s*Value\\s*Date\\s*Transaction\\s*Details\\s*Chq\\.\\s*No\\.\\s*Debit\\s*Credit\\s*Balance");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AXIS_7";
			}
			pattern = Pattern.compile("Tran\\s*Date\\s*Value\\s*Date\\s*Transaction");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("MICR\\/IFSC\\s*Code.*UTIB");
				Matcher matcher1 = pattern1.matcher(pdfText);
				Pattern pattern2 = Pattern.compile("IFSC\\s*Code.*UTIB.*MICR.*CKYC");
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher1.find()) {
					return "AXIS_5";
				} else if (matcher2.find()) {
					return "AXIS_8";
				} else {
					return "AXIS_2";
				}
			}
			pattern = Pattern.compile("Tran\\s*Date\\s*Chq\\s*No\\s*Particulars");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AXIS_3";
			}
			pattern = Pattern.compile("Date\\s*Transaction\\s*Details");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AXIS_4";
			}
			break;
		}
		// Detect ICICI
		case "ICIC": {
			pattern = Pattern.compile("IFSC\\s*CODE\\s*.*?NOMINEE[\\s\\S]*?ICIC");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "ICICI_1";
			}
			pattern = Pattern.compile("IFSC.*Nomination[\\s\\S]*?ICIC");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "ICICI_2";
			}
			pattern = Pattern.compile("IFSC\\s*Code:\\s*ICIC");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "ICICI_3";
			}
			break;
		}
		// Detect KVB
		case "KVBL": {
			pattern = Pattern.compile("IFSC\\s*CODE\\s*-\\s*KVBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "KVB_1";
			}
			break;
		}
		// Detect INDUSIND
		case "INDB": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*INDB");// 2,3,5
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("IndusInd\\s*Bank");// 3
				Pattern pattern2 = Pattern.compile("Branch\\s*IFSC\\s*Code:\\s*INDB");// 5
				Matcher matcher1 = pattern1.matcher(pdfText);
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher1.find()) {
					return "INDUS_3";
				} else if (matcher2.find()) {
					return "INDUS_5";
				} else {
					return "INDUS_2";
				}
			}
			pattern = Pattern.compile("IndusInd\\s*Bank\\s*Ltd.");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDUS_4";
			}
			break;
		}
		// Detect CUB (City Union Bank)
		case "CIUB": {
			Pattern pattern1 = Pattern.compile("Statement\\s*From\\s*:\\s*\\d{2}\\s*[A-Za-z]{3}\\s*Statement\\s*To");
			Pattern pattern2 = Pattern.compile("IFSC\\s{5,}:\\s*CIUB");
			Pattern pattern3 = Pattern.compile("Statement\\s*Dt\\s*:\\s*\\d{2}-\\w{3}-\\d{4}");
			Matcher matcher1 = pattern1.matcher(pdfText);
			Matcher matcher2 = pattern2.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			if (matcher2.find()) {
				return "CUB_2";
			} else if (matcher1.find()) {
				return "CUB_1";
			} else if (matcher3.find()) {
				return "CUB_3";
			}
			break;
		}
		// Detect DBS
		case "DBSS": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*DBSS\\w{7}[\\s\\S]*?MICR");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "DBS_1";
			}
			pattern = Pattern.compile("IFSC\\s*code\\s*:\\s*DBSS\\w{7}[\\s\\S]*?BSR");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "DBS_2";
			}
			break;
		}
		// CNTB (Central Bank)
		case "CBIN": {
			pattern = Pattern.compile("IFSC\\s*Code\\s*:\\s*CBIN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CNTB_3";
			}
			pattern = Pattern.compile("IFSC\\s*Code\\s*CBIN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CNTB_4";
			}
			break;
		}
		// Detect RBL
		case "RATN": {
			pattern = Pattern.compile("IFSC\\/RTGS\\/NEFT\\s*Code\\s*:\\s*RATN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "RBL_1";
			}
			pattern = Pattern.compile("IFSC\\/RTGS\\/NEFT\\s*:\\s*RATN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "RBL_3";
			}
			pattern = Pattern.compile("IFSC[\\s\\S]*?RATN\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "RBL_2";
			}
			break;
		}
		// Since Only 1 format is available for following banks, No need to check again,
		// Detect AUBL: AU Small Finance Bank Ltd
		case "AUBL": {
			pattern = Pattern.compile("IFSC\\s*:\\s*AUBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("IFSC\\s*:\\s*AUBL\\w{7}.*\\n.*Nominee");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "AUBL_3";
				}
				return "AUBL_1";
			}
			pattern = Pattern.compile("IFSC\s*Code.*AUBL\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AUBL_2";
			}
			break;
		}
		// Detect UJVN: Ujjivan Small Finance Bank Limited
		case "UJVN": {
			return "UJVN_1";
		}
		// Detect DLXB: Dhanlaxmi Bank
		case "DLXB": {
			return "DLXB_1";
		}
		// Detect ESAF Small Finance Bank
		case "ESMF": {
			return "ESMF_1";
		}
		// Detect SURY: SURYODAY
		case "SURY": {
			return "SURY_1";
		}
		default: {// formats which we can't detect through IFSC, (reason: IFSC not mentioned)
			// SBI
			pattern = Pattern.compile("Thank\\s*you\\s*for\\s*choosing.*services\\s*of\\s*State\\s*Bank\\s*of\\s*India");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_3";
			}
			pattern = Pattern.compile("Bank\\s*:\\s*STATE\\s*BANK\\s*OF\\s*INDIA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "SBI_10";
			}
			// CANARA
			pattern = Pattern.compile("Bank\\s*:\\s*Canara\\s*Bank");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CANARA_7";
			}
			// INDIAN ALLAHABAD
			pattern = Pattern.compile("\\n\\s*INDIAN\\s+BANK\\n\\s*Branch\\s*Code.*\\n\\s*Account");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDALH_2";
			}
			// INDIAN
			pattern = Pattern.compile("INDIAN\\s*BANK[\\s\\S]*BRANCH\\s*CODE");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDIAN_1";
			}
			// Detect AXIS
			pattern = Pattern.compile("Statement\\s*of\\s*AXIS\\s*BANK\\s*Account\\s*No\\s*:");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AXIS_1";
			}
			pattern = Pattern.compile("Statement\\s*of\\s*Axis\\s*Account\\s*No.*period");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AXIS_6";
			}
			// INDUSIND
			pattern = Pattern.compile("IndusInd\\s{10,}Bank");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDUS_1";
			}
			pattern = Pattern.compile("email\\s*to.*@indusind\\.com");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDUS_6";
			}
			pattern = Pattern.compile("I\\s*F\\s*S\\s*C\\s*.*I\\s*N\\s*D\\s*B");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "INDUS_7";
			}
			// Detect CUB (City Union Bank)
			pattern = Pattern.compile("City\\s*Union\\s*Bank", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Statement\\s*Dt\\s*:\\s*\\d{2}-\\w{3}-\\d{4}");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "CUB_3";
				}
			}
			// Detect DBS
			pattern = Pattern.compile("DBS\\s*Bank\\s*Ltd");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "DBS_3";
			}
			// Detect IDBI
			pattern = Pattern.compile("IDBI\\s*Bank\\s*Ltd");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("GSTIN:\\s*\\w*");
				Pattern pattern2 = Pattern.compile("IFSC\\s*Code\\s*:\\s*IBKL");
				Matcher matcher1 = pattern1.matcher(pdfText);
				Matcher matcher2 = pattern2.matcher(pdfText);
				if (matcher1.find()) {
					return "IDBI_1";
				} else if (matcher2.find()) {
					return "IDBI_2";
				} else {
					return "IDBI_3";
				}
			}
			pattern = Pattern.compile("IDBI\\s*BANK\\s*LTD");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("Acct\\s*Range\\s*:");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "IDBI_4";
				}
			}
			// Detect CNTB
			pattern = Pattern.compile("Cent\\s*\\w*-CBoI");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CNTB_1";
			}
			pattern = Pattern.compile("Branch\\s*E-mail\\s*:.*@centralbank.co.in");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CNTB_2";
			}
			// Detect BOB
			pattern = Pattern.compile("IFSC[\\s\\S]*?BARB\\w{7}\s+");
			matcher = pattern.matcher(pdfText);
			if (matcher.find() || pdfText.contains("https://www.bankofbaroda.in")) {
				return "BOB_3";
			}
			// Detect KOTAK Mahindra Bank
			pattern = Pattern.compile("Kotak\\s*Mahindra\\s*Bank\\s*Ltd");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "KOTAK_5";
			}
			// Detect UCO
			pattern = Pattern.compile("I\\s*F\\s*S\\s*C\\s*.*U\\s*C\\s*B\\s*A.*M\\s*I\\s*C\\s*R");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UCO_3";
			}
			pattern = Pattern.compile("E-Mail.*UCOBANK.CO.IN");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UCO_4";
			}
			// Detect MAHB
			pattern = Pattern.compile("BANK\\s*OF\\s*MAHARASHTRA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "MAHB_2";
			}
			// Detect AUBL
			pattern = Pattern.compile("I\\s*F\\s*S\\s*C\\s*.*A\\s*U\\s*B\\s*L");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "AUBL_2";
			}
			pattern = Pattern.compile("I\\s*F\\s*S\\s*C\\s*:\\s*D\\s*L\\s*X\\s*B");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "DLXB_2";
			}
			// Detect KVB NO_MARK
			pattern = Pattern.compile("Account\\s*Name.*\\n\\s*Account\\s*Number.*\\n\\s*Branch.*\\n\\s*Customer\\s*Id[\\s\\S]*?From\\s*Date\\s*\\d{2}-[A-Za-z]{3}-\\d{4}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "KVB_2";
			}
			// IOB
			pattern = Pattern.compile("IFS\\s*Code[\\s\\S]*?IOBA\\w{7}\\s+");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IOB_2";
			}
			pattern = Pattern.compile("eservice@kvbmail.com");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				Pattern pattern1 = Pattern.compile("St\\.\\s*Period\\s*:\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
				Matcher matcher1 = pattern1.matcher(pdfText);
				if (matcher1.find()) {
					return "KVB_1";
				}
				return "KVB_3";
			}
			pattern = Pattern.compile("Branch\\s*:\\s*YES\\s*BANK\\s*LTD");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "YESB_2";
			}
			pattern = Pattern.compile("IFS[\\s\\S]*?ICIC\\w{7}");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "ICICI_4";
			}
			pattern = Pattern.compile("IFSC\\/RTGS\\/NEFT\\s*code");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "RBL_1";
			}
			// Detect CSB
			pattern = Pattern.compile("CSB\\s*ePassbook[\\s\\S]*?Account\\s*Number[\\s\\S]*?Account\\s*Name[\\s\\S]*?Period\\s*From[\\s\\S]*?Period\\s*To");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "CSB_3";
			}
			pattern = Pattern.compile("Union\\s*Bank\\s*of\\s*India");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_3";
			}
			pattern = Pattern.compile("UNION\\s*BANK\\s*OF\\s*INDIA");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "UNION_5";
			}
			pattern = Pattern.compile("Statement.*?\\s+Indian\\s*Overseas\\s*Bank");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "IOB_4";
			}
			pattern = Pattern.compile("INFT\\s*-\\s*Internal\\s*Fund\\s*Transfer\\s*.*?ICICI\\s*Bank");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "ICICI_5";
			}
			pattern = Pattern.compile("Jana\\s*S\\s*m\\s*all\\s*Finan\\s*ce\\s*Ba\\s*nk\\s*Ltd");
			matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				return "JANA_1";
			}
			break;
		}
		}
		return "NONE";
	}

	@Override
	public Boolean checkFakeBankStmt(ParseBankStmtRequestDTO request, String fileUrl) throws IOException {

		String[] fakeProducersArr = new String[] { "microsoft", "convertonline", "online2pdf", "ilovepdf", "acrobat", "windows", "libreoffice", "samsung" }; // add more , eg. openhtmltopdf
		PDDocument doc = null;
		boolean isPdfFake = false;
		try {
			doc = PDDocument.load(new File(request.getFileName()));

			// use this incase of password protected file, & comment above line

			/*
			 * // Create a protection policy without any password doc = PDDocument.load(new
			 * File(request.getFileName()), request.getPassword()); AccessPermission ap =
			 * new AccessPermission(); StandardProtectionPolicy spp = new
			 * StandardProtectionPolicy(request.getPassword(), "", ap); // Apply the policy
			 * to the document doc.protect(spp);
			 * 
			 * // Save the unprotected document String fileDetails = request.getCustomerId()
			 * + "_" + "BANK_STMNT_DOC" + ".pdf"; String outputFile =
			 * "/home/shubham/Downloads/" + fileDetails; doc.save(outputFile);
			 * request.setFileName(outputFile); // END
			 */
			PDDocumentInformation pdd = doc.getDocumentInformation();

			String pdfProducer = pdd.getProducer();
			String pdfCreator = pdd.getCreator();
			log.info("Pdf Producer: " + pdfProducer + ", Creator: " + pdfCreator + ", Author: " + pdd.getAuthor() + ", Title: " + pdd.getTitle());
//          System.out.println("Creator: "+pdd.getCreator()); 
//          System.out.println("MetaData Keys: "+pdd.getMetadataKeys());
//          System.out.println("CreationDate: "+pdd.getCreationDate());
//          System.out.println("Producer: "+pdd.getProducer());
//          System.out.println("keywords: "+pdd.getKeywords());
//          System.out.println("Author: "+pdd.getAuthor());
//          System.out.println("Title: "+pdd.getTitle());
//          System.out.println("Subject: "+pdd.getSubject());
//          System.out.println("ModeDate: "+pdd.getModificationDate());

			if (pdfProducer != null && !pdfProducer.equalsIgnoreCase("")) {
				pdfProducer = pdfProducer.toLowerCase();
				for (String fakeProducer : fakeProducersArr) {
					if (pdfProducer.contains(fakeProducer)) {
						isPdfFake = true;
						break;
					}
				}
			} else if (pdfCreator != null && (pdfCreator.contains("Quadient CXM AG~Inspire") || pdfCreator.toLowerCase().contains("jasperreports"))) {
				isPdfFake = false;
			} else if (pdfProducer == null && pdfCreator == null && request.getBankCode().equalsIgnoreCase("IDFB")) {
				isPdfFake = false;
			} else {
				log.info("Pdf Producer is: NULL");
				isPdfFake = true;
			}

			if (isPdfFake) {
				log.info("DetectBankServiceImpl checkFakeStmt BankStmt Status: PDF IS FAKE.");
			} else {
				log.info("DetectBankServiceImpl checkFakeStmt BankStmt Status: PDF IS NOT FAKE.");
			}

			FakeStmtsAudit fakeStmtsAudit = new FakeStmtsAudit();
			fakeStmtsAudit.setBankName(request.getBankName());
			fakeStmtsAudit.setBankCode(request.getBankCode());
			fakeStmtsAudit.setAppName(request.getAppName());
			fakeStmtsAudit.setFilePath(request.getFileName());
			fakeStmtsAudit.setFileUrl(fileUrl);
			fakeStmtsAudit.setIsFake(isPdfFake);
			fakeStmtsAudit.setCustId(Integer.parseInt(request.getCustomerId()));
			fakeStmtsAudit.setCreator(pdfCreator);
			fakeStmtsAudit.setProducer(pdd.getProducer());
			fakeStmtsAudit.setAuthor(pdd.getAuthor());
			fakeStmtsAudit.setCreateTime(new Date());
			fakeStmtsAuditRepository.save(fakeStmtsAudit);
		} catch (InvalidPasswordException e) {
			throw e;
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in PdfServiceImpl detectFakeStmts: ", e);
		} finally {
			if (doc != null) {
				doc.close();
			}
		}
		return isPdfFake;
	}

	private String extractBankCode(String pdfText) {
		String bankCode = "";
		Pattern pattern = null;
		Matcher matcher = null;

		pattern = Pattern.compile("IFS[\\s\\S]*?([A-Z]{4})\\w{7}");
		matcher = pattern.matcher(pdfText);
		if (matcher.find()) {
			bankCode = matcher.group(1);
		}
		pattern = Pattern.compile("eservice@kvbmail.com");
		matcher = pattern.matcher(pdfText);
		if (matcher.find()) {
			bankCode = "";
		}
		log.info("DetectBankServiceImpl extractBankCode bankCode is: " + bankCode);
		return bankCode;
	}

}
