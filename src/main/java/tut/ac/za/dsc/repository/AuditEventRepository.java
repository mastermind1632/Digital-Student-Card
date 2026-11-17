package tut.ac.za.dsc.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tut.ac.za.dsc.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findAllByOrderByIdDesc(Pageable page);

    List<AuditEvent> findByTargetOrderByIdDesc(String target, Pageable page);

    @Query("""
            select a from AuditEvent a
            where :like = '%%' or lower(a.actor) like :like or lower(a.action) like :like
               or lower(a.target) like :like or lower(a.details) like :like
            order by a.id desc""")
    Page<AuditEvent> search(@Param("like") String like, Pageable page);
}
