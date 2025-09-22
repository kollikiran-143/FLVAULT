package in.fl.vault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.fl.vault.model.FakeStmtsAudit;

@Repository
public interface FakeStmtsAuditRepository extends JpaRepository<FakeStmtsAudit, Integer>{

}
