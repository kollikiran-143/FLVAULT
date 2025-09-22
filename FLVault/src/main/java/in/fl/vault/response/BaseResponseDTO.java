package in.fl.vault.response;

public class BaseResponseDTO {
	private String statusCode;
	private String statusMessage;
	private String random;
	
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	public String getRandom() {
		return random;
	}
	public void setRandom(String random) {
		this.random = random;
	}
	@Override
	public String toString() {
		return String.format("BaseResponseDTO [statusCode=%s, statusMessage=%s, random=%s]", statusCode, statusMessage,
				random);
	}
}
