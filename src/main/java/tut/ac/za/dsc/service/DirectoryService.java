package tut.ac.za.dsc.service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tut.ac.za.dsc.domain.CardStatus;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.domain.VerificationLog;
import tut.ac.za.dsc.domain.VerificationResult;
import tut.ac.za.dsc.repository.StudentRepository;
import tut.ac.za.dsc.repository.VerificationLogRepository;

/** Read-only views for staff pages: the student directory, dashboard figures and the card-check log. */
@Service
@Transactional(readOnly = true)
public class DirectoryService {

    public static final int PAGE_SIZE = 15;

    private final StudentRepository students;
    private final VerificationLogRepository checks;

    public DirectoryService(StudentRepository students, VerificationLogRepository checks) {
        this.students = students;
        this.checks = checks;
    }

    // ------------------------------------------------------------ students

    public PageView<Student> search(StudentQuery q) {
        return new PageView<>(students.findAll(spec(q), PageRequest.of(q.getPage() - 1, PAGE_SIZE, sort(q))));
    }

    public List<Student> searchAll(StudentQuery q) {
        return students.findAll(spec(q), sort(q));
    }

    private static Sort sort(StudentQuery q) {
        switch (q.getSort()) {
            case "name": return Sort.by("fullName");
            case "number": return Sort.by("studentNumber");
            case "expiry": return Sort.by("expiryDate");
            default: return Sort.by(Sort.Direction.DESC, "id");
        }
    }

    static Specification<Student> spec(StudentQuery q) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            String text = Validators.clean(q.getQ());
            if (text != null) {
                String like = "%" + text.toLowerCase().replace(" ", "%") + "%";
                where.add(cb.or(cb.like(cb.lower(root.get("fullName")), like), cb.like(root.get("studentNumber"), like),
                        cb.like(cb.lower(root.get("email")), like), cb.like(root.get("idNumber"), like)));
            }
            if (Validators.clean(q.getCampus()) != null) where.add(cb.equal(root.get("campus"), q.getCampus()));
            if (Validators.clean(q.getDepartment()) != null) where.add(cb.equal(root.get("department"), q.getDepartment()));
            LocalDate today = LocalDate.now();
            String status = q.getStatus() == null ? "" : q.getStatus().toLowerCase();
            switch (status) {
                case "pending" -> where.add(cb.equal(root.get("status"), CardStatus.PENDING));
                case "suspended" -> where.add(cb.equal(root.get("status"), CardStatus.SUSPENDED));
                case "revoked" -> where.add(cb.equal(root.get("status"), CardStatus.REVOKED));
                case "active" -> {
                    where.add(cb.equal(root.get("status"), CardStatus.ACTIVE));
                    where.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("expiryDate"), today));
                }
                case "expired" -> {
                    where.add(cb.equal(root.get("status"), CardStatus.ACTIVE));
                    where.add(cb.lessThan(root.<LocalDate>get("expiryDate"), today));
                }
                case "expiring" -> {
                    where.add(cb.equal(root.get("status"), CardStatus.ACTIVE));
                    where.add(cb.between(root.<LocalDate>get("expiryDate"), today, today.plusDays(30)));
                }
                default -> { }
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
    }

    public List<Student> pending(int max) {
        return students.findByStatusOrderByIdAsc(CardStatus.PENDING, PageRequest.of(0, max));
    }

    public long countPending() {
        return students.countByStatus(CardStatus.PENDING);
    }

    // ------------------------------------------------------------ dashboard

    public DashboardStats stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime midnight = today.atStartOfDay();
        List<NameCount> campuses = new ArrayList<>();
        for (Object[] row : students.countByCampus()) {
            campuses.add(new NameCount((String) row[0], ((Number) row[1]).longValue()));
        }
        campuses.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return new DashboardStats(
                students.count(),
                students.countValid(today),
                students.countByStatus(CardStatus.PENDING),
                students.countExpired(today),
                students.countByStatus(CardStatus.SUSPENDED) + students.countByStatus(CardStatus.REVOKED),
                students.countExpiringBetween(today, today.plusDays(30)),
                checks.countByCheckedAtGreaterThanEqual(midnight),
                checks.countByCheckedAtGreaterThanEqualAndResultNot(midnight, VerificationResult.VALID),
                withPercent(campuses),
                withPercent(registrationsByMonth(6)));
    }

    private List<NameCount> registrationsByMonth(int months) {
        YearMonth first = YearMonth.now().minusMonths(months - 1L);
        Map<YearMonth, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) buckets.put(first.plusMonths(i), new long[1]);
        for (LocalDateTime t : students.createdSince(first.atDay(1).atStartOfDay())) {
            long[] b = buckets.get(YearMonth.from(t));
            if (b != null) b[0]++;
        }
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        List<NameCount> out = new ArrayList<>();
        buckets.forEach((month, count) -> out.add(new NameCount(label.format(month), count[0])));
        return out;
    }

    private static List<NameCount> withPercent(List<NameCount> list) {
        long max = list.stream().mapToLong(NameCount::getCount).max().orElse(0);
        list.forEach(n -> n.setPercent(max == 0 ? 0 : (int) Math.round(n.getCount() * 100.0 / max)));
        return list;
    }

    // ------------------------------------------------------------ card checks

    public List<VerificationLog> recentChecks(int max) {
        return checks.findAllByOrderByIdDesc(PageRequest.of(0, max));
    }

    public List<VerificationLog> checksFor(String studentNumber, int max) {
        return checks.findByStudentNumberOrderByIdDesc(studentNumber, PageRequest.of(0, max));
    }

    public long countChecksFor(String studentNumber) {
        return checks.countByStudentNumber(studentNumber);
    }

    /** result: "passed", "failed" or empty for all. */
    public PageView<VerificationLog> searchChecks(String text, String result, int page) {
        String t = text == null ? "" : text.trim().toLowerCase();
        String r = "passed".equals(result) || "failed".equals(result) ? result : "all";
        return new PageView<>(checks.search(t, "%" + t + "%", r, PageRequest.of(Math.max(page, 1) - 1, 25)));
    }
}
