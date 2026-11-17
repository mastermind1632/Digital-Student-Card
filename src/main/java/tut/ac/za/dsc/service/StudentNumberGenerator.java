package tut.ac.za.dsc.service;

import java.security.SecureRandom;
import java.time.Year;
import org.springframework.stereotype.Component;
import tut.ac.za.dsc.repository.StudentRepository;

/** Unique 9-digit student numbers: the two-digit registration year followed by 7 random digits, e.g. 26 4810375. */
@Component
public class StudentNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final StudentRepository students;

    public StudentNumberGenerator(StudentRepository students) {
        this.students = students;
    }

    public String next() {
        String prefix = String.format("%02d", Year.now().getValue() % 100);
        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = prefix + String.format("%07d", RANDOM.nextInt(10_000_000));
            if (candidate.charAt(2) != '0' && !students.existsByStudentNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find a free student number");
    }
}
