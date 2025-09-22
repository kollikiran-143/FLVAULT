package in.fl.vault.service;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import in.fl.vault.model.FlvaultAudit;
import in.fl.vault.repository.FlvaultAuditRepository;
import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.ParseBankStmtResponseDTO;
import in.fl.vault.utils.CommonUtils;
import in.fl.vault.utils.WebConstants;

@Service
public class StmtServiceImpl implements StmtService {

	@Autowired
	private DetectBankService pdfService;
	@Autowired
	private SBIService sbiService;
	@Autowired
	private HDFCService hdfcService;
	@Autowired
	private CanaraService canaraService;
	@Autowired
	private DBSService dbsService;
	@Autowired
	private TMBService tmbService;
	@Autowired
	private IDFCService idfcService;
	@Autowired
	private UNIONService unionService;
	@Autowired
	private INDALHService indalhService;
	@Autowired
	private IndianService indianService;
	@Autowired
	private CNTBService cntbService;
	@Autowired
	private AXISService axisService;
	@Autowired
	private KNBService knbService;
	@Autowired
	private CUBService cubService;
	@Autowired
	private IOBService iobService;
	@Autowired
	private EquitasService equitasService;
	@Autowired
	private UCOService ucoService;
	@Autowired
	private IDBIService idbiService;
	@Autowired
	private IndusIndService indusIndService;
	@Autowired
	private BOBService bobService;
	@Autowired
	private PNBService pnbService;
	@Autowired
	private KOTAKService kotakService;
	@Autowired
	private ICICIService iciciService;
	@Autowired
	private BandhanService bandhanService;
	@Autowired
	private FincareService fincareService;
	@Autowired
	private StandardCharteredService standardCharteredService;
	@Autowired
	private SIBService sibService;
	@Autowired
	private CSBService csbService;
	@Autowired
	private FederalService federalService;
	@Autowired
	private BOIService boiService;
	@Autowired
	private RBLService rblService;
	@Autowired
	private MAHBService bomService;
	@Autowired
	private PSIBService psibService;
	@Autowired
	private KVBService kvbService;
	@Autowired
	private YESBService yesbService;
	@Autowired
	private AUBLService aublService;
	@Autowired
	private UJVNService ujvnService;
	@Autowired
	private DLXBService dlxbService;
	@Autowired
	private ESMFService esmfService;
	@Autowired
	private SURYService suryService;
	@Autowired
	private AwsS3Service awsS3Service;
	@Autowired
	private FlvaultAuditRepository flvaultAuditRepository;

	@Value("${spring.profiles.active}")
	private String activeProfile;

	private final Logger log = Logger.getLogger(StmtServiceImpl.class);

	@Override
	public ParseBankStmtResponseDTO parseStatement(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering StmntServiceImpl parseStatement with request: " + request);

		ParseBankStmtResponseDTO response = new ParseBankStmtResponseDTO();
		BSInfo bankStmtInfo = new BSInfo();
		String bankFormat = null;

		FlvaultAudit flvaultAudit = new FlvaultAudit();
		flvaultAudit.setCustId(Integer.parseInt(request.getCustomerId()));
		flvaultAudit.setAppName(request.getAppName());
		flvaultAudit.setFilePath(request.getFileName());
		flvaultAudit.setBankName(request.getBankName());
		String fileUrl = request.getFileName();
		try {
//			 for AWS
			log.info("StmntServiceImpl parseStatement activeProfile: " + activeProfile);
			if (activeProfile.equalsIgnoreCase("prod")) {
				String filePath = awsS3Service.getFileFromS3(request.getFileName(), request.getCustomerId());
				request.setFileName(filePath);
			}

			boolean isPdfFake = pdfService.checkFakeBankStmt(request, fileUrl); // false means: PDF is not Fake

			if (!isPdfFake) {
				bankFormat = pdfService.detectBank(request.getFileName());
				// Special case for CSB_3, IFSC of different bank in present in Description,
				// leading to incorrect format detection
				if (bankFormat.equalsIgnoreCase("NONE") && request.getBankCode() != null) {
					if (request.getBankCode().equalsIgnoreCase("CSBK")) {
						bankFormat = "CSB_3";
					} else if (request.getBankCode().equalsIgnoreCase("RATN")) {
						bankFormat = "RBL_4";
					}
				} else if (!bankFormat.equalsIgnoreCase("NONE")) {
					flvaultAudit.setBankName(CommonUtils.getBankName(bankFormat));
					flvaultAudit.setFormat(bankFormat);
				}
				log.info("StmntServiceImpl parseStatement bankFormat: " + bankFormat);

				try {
					switch (bankFormat) {
					case "SBI_1": {
						bankStmtInfo = sbiService.parseSBI1(request);
						break;
					}
					case "SBI_2": {
						bankStmtInfo = sbiService.parseSBI2(request);
						break;
					}
					case "SBI_3": {
						bankStmtInfo = sbiService.parseSBI3(request);
						break;
					}
					case "SBI_4": {
						bankStmtInfo = sbiService.parseSBI4(request);
						break;
					}
					case "SBI_5": {
						bankStmtInfo = sbiService.parseSBI5(request);
						break;
					}
					case "SBI_6": {
						bankStmtInfo = sbiService.parseSBI6(request);
						break;
					}
					case "SBI_7": {
						bankStmtInfo = sbiService.parseSBI7(request);
						break;
					}
					case "SBI_8": {
						bankStmtInfo = sbiService.parseSBI8(request);
						break;
					}
					case "SBI_9": {
						bankStmtInfo = sbiService.parseSBI9(request);
						break;
					}
					case "SBI_10": {
						bankStmtInfo = sbiService.parseSBI10(request);
						break;
					}
					case "HDFC_1": {
						bankStmtInfo = hdfcService.parseHDFC1(request);
						break;
					}
					case "HDFC_2": {
						bankStmtInfo = hdfcService.parseHDFC2(request);
						break;
					}
					case "HDFC_3": {
						bankStmtInfo = hdfcService.parseHDFC3(request);
						break;
					}
					case "CANARA_1": {
						bankStmtInfo = canaraService.parseCanara1(request);
						break;
					}
					case "CANARA_4": {
						bankStmtInfo = canaraService.parseCanara4(request);
						break;
					}
					case "CANARA_5": {
						bankStmtInfo = canaraService.parseCanara5(request);
						break;
					}
					case "CANARA_6": {
						bankStmtInfo = canaraService.parseCanara6(request);
						break;
					}
					case "CANARA_7": {
						bankStmtInfo = canaraService.parseCanara7(request);
						break;
					}
					case "DBS_1": {
						bankStmtInfo = dbsService.parseDBS1(request);
						break;
					}
					case "DBS_2": {
						bankStmtInfo = dbsService.parseDBS2(request);
						break;
					}
					case "DBS_3": {
						bankStmtInfo = dbsService.parseDBS3(request);
						break;
					}
					case "TMB_1": {
						bankStmtInfo = tmbService.parseTMB1(request);
						break;
					}
					case "TMB_2": {
						bankStmtInfo = tmbService.parseTMB2(request);
						break;
					}
					case "TMB_3": {
						bankStmtInfo = tmbService.parseTMB3(request);
						break;
					}
					case "TMB_4": {
						bankStmtInfo = tmbService.parseTMB4(request);
						break;
					}
					case "TMB_5": {
						bankStmtInfo = tmbService.parseTMB5(request);
						break;
					}
					case "TMB_6": {
						bankStmtInfo = tmbService.parseTMB6(request);
						break;
					}
					case "TMB_7": {
						bankStmtInfo = tmbService.parseTMB7(request);
						break;
					}
					case "IDFC_1": {
						bankStmtInfo = idfcService.parseIDFC1(request);
						break;
					}
					case "IDFC_2": {
						bankStmtInfo = idfcService.parseIDFC2(request);
						break;
					}
					case "IDFC_3": {
						bankStmtInfo = idfcService.parseIDFC3(request);
						break;
					}
					case "UNION_1": {
						bankStmtInfo = unionService.parseUNION1(request);
						break;
					}
					case "UNION_2": {
						bankStmtInfo = unionService.parseUNION2(request);
						break;
					}
					case "UNION_3": {
						bankStmtInfo = unionService.parseUNION3(request);
						break;
					}
					case "UNION_4": {
						bankStmtInfo = unionService.parseUNION4(request);
						break;
					}
					case "UNION_5": {
						bankStmtInfo = unionService.parseUNION5(request);
						break;
					}
					case "INDALH_1": {
						bankStmtInfo = indalhService.parseINDALH1(request);
						break;
					}
					case "INDALH_2": {
						bankStmtInfo = indalhService.parseINDALH2(request);
						break;
					}
					case "INDALH_3": {
						bankStmtInfo = indalhService.parseINDALH3(request);
						break;
					}
					case "INDALH_4": {
						bankStmtInfo = indalhService.parseINDALH4(request);
						break;
					}
					case "INDIAN_1": {
						bankStmtInfo = indianService.parseINDIAN1(request);
						break;
					}
					case "INDIAN_2": {
						bankStmtInfo = indianService.parseINDIAN2(request);
						break;
					}
					case "AXIS_1": {
						bankStmtInfo = axisService.parseAXIS1(request);
						break;
					}
					case "AXIS_2": {
						bankStmtInfo = axisService.parseAXIS2(request);
						break;
					}
					case "AXIS_3": {
						bankStmtInfo = axisService.parseAXIS3(request);
						break;
					}
					case "AXIS_4": {
						bankStmtInfo = axisService.parseAXIS4(request);
						break;
					}
					case "AXIS_5": {
						bankStmtInfo = axisService.parseAXIS5(request);
						break;
					}
					case "AXIS_6": {
						bankStmtInfo = axisService.parseAXIS6(request);
						break;
					}
					case "AXIS_7": {
						bankStmtInfo = axisService.parseAXIS7(request);
						break;
					}
					case "AXIS_8": {
						bankStmtInfo = axisService.parseAXIS8(request);
						break;
					}
					case "CNTB_1": {
						bankStmtInfo = cntbService.parseCNTB1(request);
						break;
					}
					case "CNTB_2": {
						bankStmtInfo = cntbService.parseCNTB2(request);
						break;
					}
					case "CNTB_3": {
						bankStmtInfo = cntbService.parseCNTB3(request);
						break;
					}
					case "CNTB_4": {
						bankStmtInfo = cntbService.parseCNTB4(request);
						break;
					}
					case "KNB_1": {
						bankStmtInfo = knbService.parseKNB1(request);
						break;
					}
					case "KNB_2": {
						bankStmtInfo = knbService.parseKNB2(request);
						break;
					}
					case "KNB_3": {
						bankStmtInfo = knbService.parseKNB3(request);
						break;
					}
					case "KNB_4": {
						bankStmtInfo = knbService.parseKNB4(request);
						break;
					}
					case "CUB_1": {
						bankStmtInfo = cubService.parseCUB1(request);
						break;
					}
					case "CUB_2": {
						bankStmtInfo = cubService.parseCUB2(request);
						break;
					}
					case "CUB_3": {
						bankStmtInfo = cubService.parseCUB3(request);
						break;
					}
					case "IOB_1": {
						bankStmtInfo = iobService.parseIOB1(request);
						break;
					}
					case "IOB_2": {
						bankStmtInfo = iobService.parseIOB2(request);
						break;
					}
					case "IOB_3": {
						bankStmtInfo = iobService.parseIOB3(request);
						break;
					}
					case "EQUITAS_1": {
						bankStmtInfo = equitasService.parseEquitas1(request);
						break;
					}
					case "EQUITAS_2": {
						bankStmtInfo = equitasService.parseEquitas2(request);
						break;
					}
					case "EQUITAS_3": {
						bankStmtInfo = equitasService.parseEquitas3(request);
						break;
					}
					case "EQUITAS_4": {
						bankStmtInfo = equitasService.parseEquitas4(request);
						break;
					}
					case "UCO_1": {
						bankStmtInfo = ucoService.parseUCO1(request);
						break;
					}
					case "UCO_2": {
						bankStmtInfo = ucoService.parseUCO2(request);
						break;
					}
					case "UCO_3": {
						bankStmtInfo = ucoService.parseUCO3(request);
						break;
					}
					case "UCO_4": {
						bankStmtInfo = ucoService.parseUCO4(request);
						break;
					}
					case "UCO_5": {
						bankStmtInfo = ucoService.parseUCO5(request);
						break;
					}
					case "IDBI_1": {
						bankStmtInfo = idbiService.parseIDBI1(request);
						break;
					}
					case "IDBI_2": {
						bankStmtInfo = idbiService.parseIDBI2(request);
						break;
					}
					case "IDBI_3": {
						bankStmtInfo = idbiService.parseIDBI3(request);
						break;
					}
					case "IDBI_4": {
						bankStmtInfo = idbiService.parseIDBI4(request);
						break;
					}
					case "INDUS_1": {
						bankStmtInfo = indusIndService.parseIndusInd1(request);
						break;
					}
					case "INDUS_2": {
						bankStmtInfo = indusIndService.parseIndusInd2(request);
						break;
					}
					case "INDUS_3": {
						bankStmtInfo = indusIndService.parseIndusInd3(request);
						break;
					}
					case "INDUS_4": {
						bankStmtInfo = indusIndService.parseIndusInd4(request);
						break;
					}
					case "INDUS_5": {
						bankStmtInfo = indusIndService.parseIndusInd5(request);
						break;
					}
					case "INDUS_6": {
						bankStmtInfo = indusIndService.parseIndusInd6(request);
						break;
					}
					case "INDUS_7": {
						bankStmtInfo = indusIndService.parseIndusInd7(request);
						break;
					}
					case "BOB_1": {
						bankStmtInfo = bobService.parseBOB1(request);
						break;
					}
					case "BOB_2": {
						bankStmtInfo = bobService.parseBOB2(request);
						break;
					}
					case "BOB_3": {
						bankStmtInfo = bobService.parseBOB3(request);
						break;
					}
					case "BOB_4": {
						bankStmtInfo = bobService.parseBOB4(request);
						break;
					}
					case "PNB_1": {
						bankStmtInfo = pnbService.parsePNB1(request);
						break;
					}
					case "PNB_2": {
						bankStmtInfo = pnbService.parsePNB2(request);
						break;
					}
					case "PNB_3": {
						bankStmtInfo = pnbService.parsePNB3(request);
						break;
					}
					case "PNB_4": {
						bankStmtInfo = pnbService.parsePNB4(request);
						break;
					}
					case "PNB_5": {
						bankStmtInfo = pnbService.parsePNB5(request);
						break;
					}
					case "KOTAK_1": {
						bankStmtInfo = kotakService.parseKOTAK1(request);
						break;
					}
					case "KOTAK_2": {
						bankStmtInfo = kotakService.parseKOTAK2(request);
						break;
					}
					case "KOTAK_3": {
						bankStmtInfo = kotakService.parseKOTAK3(request);
						break;
					}
					case "KOTAK_4": {
						bankStmtInfo = kotakService.parseKOTAK4(request);
						break;
					}
					case "KOTAK_5": {
						bankStmtInfo = kotakService.parseKOTAK5(request);
						break;
					}
					case "ICICI_1": {
						bankStmtInfo = iciciService.parseICICI1(request);
						break;
					}
					case "ICICI_2": {
						bankStmtInfo = iciciService.parseICICI2(request);
						break;
					}
					case "ICICI_3": {
						bankStmtInfo = iciciService.parseICICI3(request);
						break;
					}
					case "ICICI_4": {
						bankStmtInfo = iciciService.parseICICI4(request);
						break;
					}
					case "BANDHAN_1": {
						bankStmtInfo = bandhanService.parseBandhan1(request);
						break;
					}
					case "BANDHAN_2": {
						bankStmtInfo = bandhanService.parseBandhan2(request);
						break;
					}
					case "BANDHAN_3": {
						bankStmtInfo = bandhanService.parseBandhan3(request);
						break;
					}
					case "FINCARE_1": {
						bankStmtInfo = fincareService.parseFincare1(request);
						break;
					}
					case "STND_1": {
						bankStmtInfo = standardCharteredService.parseSTND1(request);
						break;
					}
					case "STND_2": {
						bankStmtInfo = standardCharteredService.parseSTND2(request);
						break;
					}
					case "STND_3": {
						bankStmtInfo = standardCharteredService.parseSTND3(request);
						break;
					}
					case "SIB_1": {
						bankStmtInfo = sibService.parseSIB1(request);
						break;
					}
					case "SIB_2": {
						bankStmtInfo = sibService.parseSIB2(request);
						break;
					}
					case "SIB_3": {
						bankStmtInfo = sibService.parseSIB3(request);
						break;
					}
					case "SIB_4": {
						bankStmtInfo = sibService.parseSIB4(request);
						break;
					}
					case "CSB_1": {
						bankStmtInfo = csbService.parseCSB1(request);
						break;
					}
					case "CSB_2": {
						bankStmtInfo = csbService.parseCSB2(request);
						break;
					}
					case "CSB_3": {
						bankStmtInfo = csbService.parseCSB3(request);
						break;
					}
					case "CSB_4": {
						bankStmtInfo = csbService.parseCSB4(request);
						break;
					}
					case "CSB_5": {
						bankStmtInfo = csbService.parseCSB5(request);
						break;
					}
					case "CSB_6": {
						bankStmtInfo = csbService.parseCSB6(request);
						break;
					}
					case "CSB_7": {
						bankStmtInfo = csbService.parseCSB7(request);
						break;
					}
					case "CSB_8": {
						bankStmtInfo = csbService.parseCSB8(request);
						break;
					}
					case "FEDERAL_1": {
						bankStmtInfo = federalService.parseFederal1(request);
						break;
					}
					case "FEDERAL_2": {
						bankStmtInfo = federalService.parseFederal2(request);
						break;
					}
					case "BOI_1": {
						bankStmtInfo = boiService.parseBOI1(request);
						break;
					}
					case "BOI_2": {
						bankStmtInfo = boiService.parseBOI2(request);
						break;
					}
					case "RBL_1": {
						bankStmtInfo = rblService.parseRBL1(request);
						break;
					}
					case "RBL_2": {
						bankStmtInfo = rblService.parseRBL2(request);
						break;
					}
					case "RBL_3": {
						bankStmtInfo = rblService.parseRBL3(request);
						break;
					}
					case "RBL_4": {
						bankStmtInfo = rblService.parseRBL4(request);
						break;
					}
					case "MAHB_1": {
						bankStmtInfo = bomService.parseMAHB1(request);
						break;
					}
					case "MAHB_2": {
						bankStmtInfo = bomService.parseMAHB2(request);
						break;
					}
					case "PSIB_1": {
						bankStmtInfo = psibService.parsePSIB1(request);
						break;
					}
					case "PSIB_2": {
						bankStmtInfo = psibService.parsePSIB2(request);
						break;
					}
					case "KVB_1": {
						bankStmtInfo = kvbService.parseKVB1(request);
						break;
					}
					case "KVB_2": {
						bankStmtInfo = kvbService.parseKVB2(request);
						break;
					}
					case "KVB_3": {
						bankStmtInfo = kvbService.parseKVB3(request);
						break;
					}
					case "YESB_1": {
						bankStmtInfo = yesbService.parseYESB1(request);
						break;
					}
					case "YESB_2": {
						bankStmtInfo = yesbService.parseYESB2(request);
						break;
					}
					case "YESB_3": {
						bankStmtInfo = yesbService.parseYESB3(request);
						break;
					}
					case "AUBL_1": {
						bankStmtInfo = aublService.parseAUBL1(request);
						break;
					}
					case "AUBL_2": {
						bankStmtInfo = aublService.parseAUBL2(request);
						break;
					}
					case "AUBL_3": {
						bankStmtInfo = aublService.parseAUBL3(request);
						break;
					}
					case "UJVN_1": {
						bankStmtInfo = ujvnService.parseUJVN1(request);
						break;
					}
					case "DLXB_1": {
						bankStmtInfo = dlxbService.parseDLXB1(request);
						break;
					}
					case "DLXB_2": {
						bankStmtInfo = dlxbService.parseDLXB2(request);
						break;
					}
					case "ESMF_1": {
						bankStmtInfo = esmfService.parseESMF1(request);
						break;
					}
					case "SURY_1": {
						bankStmtInfo = suryService.parseSURY1(request);
						break;
					}
					default:
						throw new IllegalArgumentException("No Method to Parse: Unknown BankFormat==> " + bankFormat);
					}
					if (bankStmtInfo != null && bankStmtInfo.getTransactions() != null && !bankStmtInfo.getTransactions().isEmpty()) {
						flvaultAudit.setAccNo(bankStmtInfo.getAccountNo());
						flvaultAudit.setIfsc(bankStmtInfo.getIfsc());
						flvaultAudit.setPan(bankStmtInfo.getPan());
						bankStmtInfo.setName(bankStmtInfo.getName().replaceAll("['\"]", ""));
						String validateCheckRes = validateChecks(bankStmtInfo.toString(), bankStmtInfo.getTransactions().size());
						if (!validateCheckRes.equalsIgnoreCase("")) {
							log.info("validateCheckRespose: " + validateCheckRes);
							throw new Exception(validateCheckRes);
						}
						flvaultAudit.setStatus(WebConstants.SUCCESS_MSG);
						bankStmtInfo.setStmntFormat(bankFormat.equalsIgnoreCase("NONE") ? request.getBankCode() + "_NA" : bankFormat);
						List<BSInfo> bankStmtInfoList = new ArrayList<>();
						bankStmtInfoList.add(bankStmtInfo);

						response.setBankStatements(bankStmtInfoList);
						response.setStatusCode(WebConstants.SUCCESS_CODE);
						response.setStatusMessage(WebConstants.SUCCESS_MSG);
					} else {
						throw new Exception("Transactions are Null/Empty for pdf with format==> " + bankFormat);
					}
				} catch (Exception e) {
					// e.printStackTrace();
					flvaultAudit.setStatus(WebConstants.FAILURE_MSG);
					flvaultAudit.setStatusMsg(e.getMessage());
					response.setStatusCode(WebConstants.FAILURE_CODE);
					response.setStatusMessage(WebConstants.FAILURE_MSG);
					response.setRandom(e.getMessage());
					log.error("Error in StmntServiceImpl parseStatement with parsing pdf: " + e.getMessage());
				}
			} else {
				flvaultAudit.setStatus("BankStmt is FAKE.");
				response.setStatusCode(WebConstants.FAILURE_CODE);
				response.setStatusMessage("BankStmt is FAKE.");
			}
		} catch (Exception e) {
			flvaultAudit.setStatus(WebConstants.FAILURE_MSG);
			response.setStatusCode(WebConstants.FAILURE_CODE);
			response.setStatusMessage(WebConstants.FAILURE_MSG);
			response.setRandom(e.getMessage());
			log.error("Error in detecting bank name: " + e);
		}
		flvaultAuditRepository.save(flvaultAudit);
		// delete the downloaded file
		if (activeProfile.equalsIgnoreCase("prod")) {
			removeFile(request);
		}
		log.info("Exiting StmntServiceImpl parseStatement");
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for StmntServiceImpl parseStatement is ==> " + timeTaken);
		return response;
	}

	private String validateChecks(String txnsString, int txnCount) {

		String nameCheck1 = "name=,", nameCheck2 = "name=null";
		String accNoCheck1 = "accountNo=,", accNoCheck2 = "accountNo=null";
		String txnCheck = "debit=, credit=,";
		Pattern txnDateCountPattern = Pattern.compile("txnDate=\\d{4}-\\d{2}-\\d{2}");
		Pattern amountPattern = Pattern.compile("amount=[A-Za-z]+");
		Pattern dateCheck = Pattern.compile("(txn|start|en)Date=(null)?,");

		Matcher dateMatcher = dateCheck.matcher(txnsString);
		Matcher txnDateCountMatcher = txnDateCountPattern.matcher(txnsString);
		Matcher amountMatcher = amountPattern.matcher(txnsString);

		if (txnsString.contains(accNoCheck1) || txnsString.contains(accNoCheck2)) {
			return "AccountNo is Empty or null!";
		} else if (txnsString.contains(nameCheck1) || txnsString.contains(nameCheck2)) {
			return "Name is Empty or null!";
		} else if (dateMatcher.find()) {
			return "txnDate/startDate/endDate is Empty or null!";
		} else if (txnsString.contains(txnCheck)) {
			return "Debit and Credit both are Empty!";
		} else if (amountMatcher.find()) {
			return "amount contains alpha characters!";
		}
		if (txnDateCountMatcher.find()) {
			int count = 1;
			while (txnDateCountMatcher.find())
				count++;
			if (count != txnCount) {
				return "txnDatePattern MatchCount: " + count + " is not equal to TxnCount: " + txnCount;
			}
		}
		return "";
	}

	public void removeFile(ParseBankStmtRequestDTO requestDTO) {
		try {
			log.info(requestDTO.getFileName());
			Path path = Paths.get(requestDTO.getFileName());
			Files.deleteIfExists(path);
			log.info("Pdf File deleted Successfully.");
		} catch (FileNotFoundException e) {
			log.info("File Not Found." + requestDTO.getFileName());
		} catch (Exception e) {
			log.error("Error in deleting File");
		}
	}
}
