package in.fl.vault.response;

public class ExternalApiFunnelResponseDTO extends BaseResponseDTO{
	private String requested;
	private String success;
	private String failure;
	
	public String getRequested() {
		return requested;
	}
	public void setRequested(String requested) {
		this.requested = requested;
	}
	public String getSuccess() {
		return success;
	}
	public void setSuccess(String success) {
		this.success = success;
	}
	public String getFailure() {
		return failure;
	}
	public void setFailure(String failure) {
		this.failure = failure;
	}
	@Override
	public String toString() {
		return String.format(
				"ExternalApiFunnelResponseDTO [requested=%s, success=%s, failure=%s, getStatusCode()=%s, getStatusMessage()=%s, getRandom()=%s]",
				requested, success, failure, getStatusCode(), getStatusMessage(), getRandom());
	}
}
