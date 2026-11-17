package tut.ac.za.dsc.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tut.ac.za.dsc.domain.VerificationLog;
import tut.ac.za.dsc.domain.VerificationResult;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    List<VerificationLog> findByStudentNumberOrderByIdDesc(String studentNumber, Pageable page);

    long countByStudentNumber(String studentNumber);

    long countByCheckedAtGreaterThanEqual(LocalDateTime since);

    long countByCheckedAtGreaterThanEqualAndResultNot(LocalDateTime since, VerificationResult result);

    List<VerificationLog> findAllByOrderByIdDesc(Pageable page);

    /** passed: null for all results, true for valid checks only, false for the rest. */
    @Query("""
            select v from VerificationLog v
            where (:text = '' or v.studentNumber like :like or lower(v.studentName) like :like or lower(v.checkedBy) like :like)
              and (:result = 'all' or (:result = 'passed' and v.result = tut.ac.za.dsc.domain.VerificationResult.VALID)
                   or (:result = 'failed' and v.result <> tut.ac.za.dsc.domain.VerificationResult.VALID))
            order by v.id desc""")
    Page<VerificationLog> search(@Param("text") String text, @Param("like") String like, @Param("result") String result, Pageable page);
}
