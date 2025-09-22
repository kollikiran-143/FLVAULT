// Generated with g9.

package in.fl.vault.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;


@Entity(name="flvault_audit")
public class FlvaultAudit implements Serializable {

    private static final long serialVersionUID = 1025737592436165485L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(unique=true, nullable=false, precision=11)
    private int id;
    @Column(name="cust_id", precision=11)
    private int custId;
    @Column(name="bank_name", length=20)
    private String bankName;
    @Column(length=11)
    private String ifsc;
    @Column(name="acc_no", length=25)
    private String accNo;
    @Column(length=11)
    private String pan;
    @Column(length=20)
    private String format;
    @Column(name="file_path", length=200)
    private String filePath;
    @Column(length=20)
    private String status;
    @Column(name="status_msg", length=250)
    private String statusMsg;
    @Column(name="app_name", length=11)
    private String appName;
    @Transient
    @Column(name="create_time")
    private Date createTime;
    @Transient
    @Column(name="modified_time")
    private Date modifiedTime;

    /** Default constructor. */
    public FlvaultAudit() {
        super();
    }

    /**
     * Access method for id.
     *
     * @return the current value of id
     */
    public int getId() {
        return id;
    }

    /**
     * Setter method for id.
     *
     * @param aId the new value for id
     */
    public void setId(int aId) {
        id = aId;
    }

    /**
     * Access method for custId.
     *
     * @return the current value of custId
     */
    public int getCustId() {
        return custId;
    }

    /**
     * Setter method for custId.
     *
     * @param aCustId the new value for custId
     */
    public void setCustId(int aCustId) {
        custId = aCustId;
    }

    /**
     * Access method for bankName.
     *
     * @return the current value of bankName
     */
    public String getBankName() {
        return bankName;
    }

    /**
     * Setter method for bankName.
     *
     * @param aBankName the new value for bankName
     */
    public void setBankName(String aBankName) {
        bankName = aBankName;
    }

    /**
     * Access method for ifsc.
     *
     * @return the current value of ifsc
     */
    public String getIfsc() {
        return ifsc;
    }

    /**
     * Setter method for ifsc.
     *
     * @param aIfsc the new value for ifsc
     */
    public void setIfsc(String aIfsc) {
        ifsc = aIfsc;
    }

    /**
     * Access method for accNo.
     *
     * @return the current value of accNo
     */
    public String getAccNo() {
        return accNo;
    }

    /**
     * Setter method for accNo.
     *
     * @param aAccNo the new value for accNo
     */
    public void setAccNo(String aAccNo) {
        accNo = aAccNo;
    }

    /**
     * Access method for pan.
     *
     * @return the current value of pan
     */
    public String getPan() {
        return pan;
    }

    /**
     * Setter method for pan.
     *
     * @param aPan the new value for pan
     */
    public void setPan(String aPan) {
        pan = aPan;
    }

    /**
     * Access method for format.
     *
     * @return the current value of format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Setter method for format.
     *
     * @param aFormat the new value for format
     */
    public void setFormat(String aFormat) {
        format = aFormat;
    }

    /**
     * Access method for filePath.
     *
     * @return the current value of filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Setter method for filePath.
     *
     * @param aFilePath the new value for filePath
     */
    public void setFilePath(String aFilePath) {
        filePath = aFilePath;
    }

    /**
     * Access method for status.
     *
     * @return the current value of status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Setter method for status.
     *
     * @param aStatus the new value for status
     */
    public void setStatus(String aStatus) {
        status = aStatus;
    }
    

    public String getStatusMsg() {
		return statusMsg;
	}

	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}

	/**
     * Access method for appName.
     *
     * @return the current value of appName
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Setter method for appName.
     *
     * @param aAppName the new value for appName
     */
    public void setAppName(String aAppName) {
        appName = aAppName;
    }

    /**
     * Access method for createTime.
     *
     * @return the current value of createTime
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Setter method for createTime.
     *
     * @param aCreateTime the new value for createTime
     */
    public void setCreateTime(Date aCreateTime) {
        createTime = aCreateTime;
    }

    /**
     * Access method for modifiedTime.
     *
     * @return the current value of modifiedTime
     */
    public Date getModifiedTime() {
        return modifiedTime;
    }

    /**
     * Setter method for modifiedTime.
     *
     * @param aModifiedTime the new value for modifiedTime
     */
    public void setModifiedTime(Date aModifiedTime) {
        modifiedTime = aModifiedTime;
    }

	@Override
	public int hashCode() {
		return Objects.hash(accNo, appName, bankName, createTime, custId, filePath, format, id, ifsc, modifiedTime, pan,
				status, statusMsg);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlvaultAudit other = (FlvaultAudit) obj;
		return Objects.equals(accNo, other.accNo) && Objects.equals(appName, other.appName)
				&& Objects.equals(bankName, other.bankName) && Objects.equals(createTime, other.createTime)
				&& custId == other.custId && Objects.equals(filePath, other.filePath)
				&& Objects.equals(format, other.format) && id == other.id && Objects.equals(ifsc, other.ifsc)
				&& Objects.equals(modifiedTime, other.modifiedTime) && Objects.equals(pan, other.pan)
				&& Objects.equals(status, other.status) && Objects.equals(statusMsg, other.statusMsg);
	}

	@Override
	public String toString() {
		return String.format(
				"FlvaultAudit [id=%s, custId=%s, bankName=%s, ifsc=%s, accNo=%s, pan=%s, format=%s, filePath=%s, status=%s, statusMsg=%s, appName=%s, createTime=%s, modifiedTime=%s]",
				id, custId, bankName, ifsc, accNo, pan, format, filePath, status, statusMsg, appName, createTime,
				modifiedTime);
	}

}
