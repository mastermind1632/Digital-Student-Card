package tut.ac.za.dsc.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;

/**
 * Reads and checks a South African ID number: YYMMDD SSSS C A Z
 * (date of birth, gender sequence, citizenship, a digit that is usually 8, and a Luhn check digit).
 */
public final class SAIdNumber {

    private final boolean valid;
    private final String error;
    private final LocalDate birthDate;
    private final String gender;
    private final String citizenship;
    private final String digits;

    private SAIdNumber(String digits, String error, LocalDate birthDate, String gender, String citizenship) {
        this.digits = digits;
        this.valid = error == null;
        this.error = error;
        this.birthDate = birthDate;
        this.gender = gender;
        this.citizenship = citizenship;
    }

    private static SAIdNumber invalid(String message) {
        return new SAIdNumber(null, message, null, null, null);
    }

    public static SAIdNumber parse(String raw) {
        return parse(raw, LocalDate.now());
    }

    static SAIdNumber parse(String raw, LocalDate today) {
        if (raw == null || raw.isBlank()) return invalid("Enter your 13-digit ID number.");
        String id = raw.replaceAll("\\s", "");
        if (!id.matches("\\d{13}")) return invalid("An ID number has exactly 13 digits.");
        LocalDate birth = birthDateOf(id, today);
        if (birth == null) return invalid("The first six digits are not a real date of birth (YYMMDD).");
        char citizen = id.charAt(10);
        if (citizen != '0' && citizen != '1') return invalid("Digit 11 must be 0 (citizen) or 1 (permanent resident).");
        if (!luhnValid(id)) return invalid("This ID number's check digit does not match. Check it for a typing mistake.");
        String gender = Integer.parseInt(id.substring(6, 10)) < 5000 ? "Female" : "Male";
        String citizenship = citizen == '0' ? "South African citizen" : "Permanent resident";
        return new SAIdNumber(id, null, birth, gender, citizenship);
    }

    /** YY is read as 20YY unless that date is in the future, then as 19YY. */
    static LocalDate birthDateOf(String id, LocalDate today) {
        int yy = Integer.parseInt(id.substring(0, 2));
        int mm = Integer.parseInt(id.substring(2, 4));
        int dd = Integer.parseInt(id.substring(4, 6));
        try {
            LocalDate in2000s = LocalDate.of(2000 + yy, mm, dd);
            return in2000s.isAfter(today) ? LocalDate.of(1900 + yy, mm, dd) : in2000s;
        } catch (DateTimeException e) {
            return null;
        }
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (dbl) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    public boolean isValid() { return valid; }
    public String getError() { return error; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getGender() { return gender; }
    public String getCitizenship() { return citizenship; }
    /** The 13 digits without spaces. */
    public String getDigits() { return digits; }

    public int getAge() {
        return birthDate == null ? 0 : Period.between(birthDate, LocalDate.now()).getYears();
    }
}
