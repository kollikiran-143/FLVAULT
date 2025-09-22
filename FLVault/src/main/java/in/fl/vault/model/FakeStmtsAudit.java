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


@Entity(name="fake_stmts_audit")
public class FakeStmtsAudit implements Serializable {

    private static final long serialVersionUID = 7999333889890065341L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(unique=true, nullable=false)
    private int id;
    @Column(name="cust_id")
    private int custId;
    @Column(name="bank_name", length=50)
    private String bankName;
    @Column(name="bank_code", length=11)
    private String bankCode;
    @Column(length=100)
    private String producer;
    @Column(length=100)
    private String creator;
    @Column(length=100)
    private String author;
    @Column(length=50)
    private String hashval;
    @Column(name="file_path")
    private String filePath;
    @Column(name="file_url")
    private String fileUrl;
    @Column(name="app_name", length=11)
    private String appName;
    @Column(name="is_fake", precision=4)
    private boolean isFake;
    @Transient
    @Column(name="create_time")
    private Date createTime;
    @Transient
    @Column(name="modified_time")
    private Date modifiedTime;

    /** Default constructor. */
    public FakeStmtsAudit() {
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
     * Access method for bankCode.
     *
     * @return the current value of bankCode
     */
    public String getBankCode() {
        return bankCode;
    }

    /**
     * Setter method for bankCode.
     *
     * @param aBankCode the new value for bankCode
     */
    public void setBankCode(String aBankCode) {
        bankCode = aBankCode;
    }

    /**
     * Access method for producer.
     *
     * @return the current value of producer
     */
    public String getProducer() {
        return producer;
    }

    /**
     * Setter method for producer.
     *
     * @param aProducer the new value for producer
     */
    public void setProducer(String aProducer) {
        producer = aProducer;
    }

    /**
     * Access method for creator.
     *
     * @return the current value of creator
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Setter method for creator.
     *
     * @param aCreator the new value for creator
     */
    public void setCreator(String aCreator) {
        creator = aCreator;
    }

    /**
     * Access method for author.
     *
     * @return the current value of author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Setter method for author.
     *
     * @param aAuthor the new value for author
     */
    public void setAuthor(String aAuthor) {
        author = aAuthor;
    }

    /**
     * Access method for hashval.
     *
     * @return the current value of hashval
     */
    public String getHashval() {
        return hashval;
    }

    /**
     * Setter method for hashval.
     *
     * @param aHashval the new value for hashval
     */
    public void setHashval(String aHashval) {
        hashval = aHashval;
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
     * Access method for fileUrl.
     *
     * @return the current value of fileUrl
     */
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * Setter method for fileUrl.
     *
     * @param aFileUrl the new value for fileUrl
     */
    public void setFileUrl(String aFileUrl) {
        fileUrl = aFileUrl;
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
     * Access method for isFake.
     *
     * @return true if and only if isFake is currently true
     */
    public boolean getIsFake() {
        return isFake;
    }

    /**
     * Setter method for isFake.
     *
     * @param aIsFake the new value for isFake
     */
    public void setIsFake(boolean aIsFake) {
        isFake = aIsFake;
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
		return Objects.hash(appName, author, bankCode, bankName, createTime, creator, custId, filePath, fileUrl,
				hashval, id, isFake, modifiedTime, producer);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FakeStmtsAudit other = (FakeStmtsAudit) obj;
		return Objects.equals(appName, other.appName) && Objects.equals(author, other.author)
				&& Objects.equals(bankCode, other.bankCode) && Objects.equals(bankName, other.bankName)
				&& Objects.equals(createTime, other.createTime) && Objects.equals(creator, other.creator)
				&& custId == other.custId && Objects.equals(filePath, other.filePath)
				&& Objects.equals(fileUrl, other.fileUrl) && Objects.equals(hashval, other.hashval) && id == other.id
				&& isFake == other.isFake && Objects.equals(modifiedTime, other.modifiedTime)
				&& Objects.equals(producer, other.producer);
	}

	@Override
	public String toString() {
		return String.format(
				"FakeStmtsAudit [id=%s, custId=%s, bankName=%s, bankCode=%s, producer=%s, creator=%s, author=%s, hashval=%s, filePath=%s, fileUrl=%s, appName=%s, isFake=%s, createTime=%s, modifiedTime=%s]",
				id, custId, bankName, bankCode, producer, creator, author, hashval, filePath, fileUrl, appName, isFake,
				createTime, modifiedTime);
	}
    
}
