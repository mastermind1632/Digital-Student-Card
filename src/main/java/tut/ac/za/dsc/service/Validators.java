package tut.ac.za.dsc.service;

import java.util.regex.Pattern;

/** Input checks shared by registration, profile changes and staff forms. Each returns null when the value is fine. */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SA_PHONE = Pattern.compile("^(\\+27|0)[6-8][0-9]{8}$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'-]{1,98}[\\p{L}.]$");

    private Validators() {
    }

    public static String clean(String value) {
        if (value == null) return null;
        String v = value.trim().replaceAll("\\s+", " ");
        return v.isEmpty() ? null : v;
    }

    public static String nameProblem(String name) {
        if (name == null) return "Enter your full name.";
        if (!NAME.matcher(name).matches() || !name.contains(" ")) return "Enter your first name and surname, using letters only.";
        return null;
    }

    public static String emailProblem(String email) {
        if (email == null) return "Enter your email address.";
        return EMAIL.matcher(email).matches() && email.length() <= 120 ? null : "Enter an email address like name@example.com.";
    }

    /** Accepts 0821234567, 082 123 4567 and +27821234567. */
    public static String phoneProblem(String phone, boolean required) {
        if (phone == null) return required ? "Enter your cellphone number." : null;
        return SA_PHONE.matcher(normalisePhone(phone)).matches() ? null : "Enter a South African cellphone number, e.g. 082 123 4567.";
    }

    public static String normalisePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[\\s()-]", "");
    }

    /** At least 8 characters with upper and lower case, a number and a symbol. */
    public static String passwordProblem(String password) {
        if (password == null || password.length() < 8) return "Use at least 8 characters.";
        if (password.length() > 72) return "Use 72 characters or fewer.";
        if (!password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*")) return "Use both upper-case and lower-case letters.";
        if (!password.matches(".*\\d.*")) return "Include at least one number.";
        if (!password.matches(".*[^A-Za-z0-9].*")) return "Include at least one symbol, such as ! @ # or $.";
        if (password.matches(".*\\s.*")) return "Passwords cannot contain spaces.";
        return null;
    }
}
