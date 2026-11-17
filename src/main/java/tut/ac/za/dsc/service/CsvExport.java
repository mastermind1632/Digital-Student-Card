package tut.ac.za.dsc.service;

import java.util.List;
import tut.ac.za.dsc.domain.Student;

/** Spreadsheet-friendly export of the student directory. */
public final class CsvExport {

    private CsvExport() {
    }

    public static String students(List<Student> list) {
        StringBuilder sb = new StringBuilder("﻿"); // lets Excel open the file as UTF-8
        sb.append("Student number,Full name,Email,Phone,Campus,Faculty,Qualification,Year,Card status,Valid until,Registered\r\n");
        for (Student s : list) {
            sb.append(String.join(",", cell(s.getStudentNumber()), cell(s.getFullName()), cell(s.getEmail()), cell(s.getPhoneNumber()),
                    cell(s.getCampus()), cell(s.getDepartment()), cell(s.getQualification()),
                    cell(s.getYearOfStudy() == null ? "" : String.valueOf(s.getYearOfStudy())), cell(s.getStatusLabel()),
                    cell(s.getExpiryDateText()), cell(s.getCreatedAtText()))).append("\r\n");
        }
        return sb.toString();
    }

    static String cell(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0 && !s.matches("[+-]?\\d+")) {
            s = "'" + s; // stops spreadsheet formula injection
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
