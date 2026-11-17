package tut.ac.za.dsc.web;

import java.io.Serializable;

/** A one-time message shown at the top of the next page (after a redirect). */
public class Flash implements Serializable {

    private final String type;
    private final String message;

    private Flash(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static Flash success(String m) { return new Flash("success", m); }
    public static Flash info(String m) { return new Flash("info", m); }
    public static Flash warning(String m) { return new Flash("warning", m); }
    public static Flash error(String m) { return new Flash("error", m); }

    /** success, info, warning or error */
    public String getType() { return type; }
    public String getMessage() { return message; }
}
