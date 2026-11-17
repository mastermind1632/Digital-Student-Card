package tut.ac.za.dsc.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Search, filter, sort and paging options for the student directory (bound from the query string). */
public class StudentQuery {

    private String q;
    private String campus;
    private String department;
    /** pending, active, expiring, expired, suspended, revoked or empty for all. */
    private String status;
    /** newest, name, number or expiry. */
    private String sort = "newest";
    private int page = 1;

    /** The filters as a query string without the page number, for pager and export links. */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        add(sb, "q", q);
        add(sb, "campus", campus);
        add(sb, "department", department);
        add(sb, "status", status);
        add(sb, "sort", "newest".equals(sort) ? null : sort);
        return sb.toString();
    }

    private static void add(StringBuilder sb, String name, String value) {
        if (value == null || value.isBlank()) return;
        if (sb.length() > 0) sb.append('&');
        sb.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort == null || sort.isBlank() ? "newest" : sort; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
}
