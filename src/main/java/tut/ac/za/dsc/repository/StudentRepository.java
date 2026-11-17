package tut.ac.za.dsc.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tut.ac.za.dsc.domain.CardStatus;
import tut.ac.za.dsc.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByStudentNumber(String studentNumber);

    Optional<Student> findByIdNumber(String idNumber);

    Optional<Student> findByEmailIgnoreCase(String email);

    boolean existsByStudentNumber(String studentNumber);

    long countByStatus(CardStatus status);

    @Query("select count(s) from Student s where s.status = tut.ac.za.dsc.domain.CardStatus.ACTIVE and s.expiryDate >= :today")
    long countValid(@Param("today") LocalDate today);

    @Query("select count(s) from Student s where s.status = tut.ac.za.dsc.domain.CardStatus.ACTIVE and s.expiryDate < :today")
    long countExpired(@Param("today") LocalDate today);

    @Query("select count(s) from Student s where s.status = tut.ac.za.dsc.domain.CardStatus.ACTIVE and s.expiryDate between :today and :until")
    long countExpiringBetween(@Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("select s.campus, count(s) from Student s group by s.campus")
    List<Object[]> countByCampus();

    @Query("select s.createdAt from Student s where s.createdAt >= :from")
    List<LocalDateTime> createdSince(@Param("from") LocalDateTime from);

    List<Student> findByStatusOrderByIdAsc(CardStatus status, Pageable page);
}
