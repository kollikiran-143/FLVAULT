package in.fl.vault.request;

public class BaseRequestDTO {
	private String appName;
	private String customerId;
	private String random;
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getRandom() {
		return random;
	}
	public void setRandom(String random) {
		this.random = random;
	}
	@Override
	public String toString() {
		return String.format("BaseRequestDTO [appName=%s, customerId=%s, random=%s]", appName, customerId, random);
	}
}
