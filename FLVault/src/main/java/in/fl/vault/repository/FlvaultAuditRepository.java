package in.fl.vault.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import in.fl.vault.model.FlvaultAudit;

@Repository
public interface FlvaultAuditRepository extends JpaRepository<FlvaultAudit, Integer>{
	
	@Query(value = "select count(*) from flvault_audit where date(create_time)=:createTime", nativeQuery = true)
	public int totalRequested(Date createTime);

	@Query(value = "select count(*) from flvault_audit where status = 'SUCCESS' and date(create_time)=:createTime", nativeQuery = true)
	public int totalSuccessCount(Date createTime);
}
