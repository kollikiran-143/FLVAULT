package in.fl.vault.request;

public class VGParserFunnelRequestDTO {
	public String reportDate;

	public String getReportDate() {
		return reportDate;
	}

	public void setReportDate(String reportDate) {
		this.reportDate = reportDate;
	}

	@Override
	public String toString() {
		return String.format("ExternalParseBankStmtApiFunnelRequestDTO [reportDate=%s]", reportDate);
	}
}
