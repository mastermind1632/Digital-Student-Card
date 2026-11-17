package tut.ac.za.dsc.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Date formats used on pages, cards and emails. */
public final class Formats {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private Formats() {
    }

    public static String day(LocalDate d) {
        return d == null ? null : DAY.format(d);
    }

    public static String longDay(LocalDate d) {
        return d == null ? null : LONG_DAY.format(d);
    }

    public static String dayTime(LocalDateTime t) {
        return t == null ? null : DAY_TIME.format(t);
    }
}
