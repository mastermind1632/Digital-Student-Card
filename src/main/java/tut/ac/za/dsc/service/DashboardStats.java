package tut.ac.za.dsc.service;

import java.util.List;

/** Figures for the administrator's overview page. */
public class DashboardStats {

    private final long total;
    private final long active;
    private final long pending;
    private final long expired;
    private final long suspended;
    private final long expiringSoon;
    private final long checksToday;
    private final long failedChecksToday;
    private final List<NameCount> byCampus;
    private final List<NameCount> registrationsByMonth;

    public DashboardStats(long total, long active, long pending, long expired, long suspended, long expiringSoon,
                          long checksToday, long failedChecksToday, List<NameCount> byCampus, List<NameCount> registrationsByMonth) {
        this.total = total;
        this.active = active;
        this.pending = pending;
        this.expired = expired;
        this.suspended = suspended;
        this.expiringSoon = expiringSoon;
        this.checksToday = checksToday;
        this.failedChecksToday = failedChecksToday;
        this.byCampus = byCampus;
        this.registrationsByMonth = registrationsByMonth;
    }

    public long getTotal() { return total; }
    public long getActive() { return active; }
    public long getPending() { return pending; }
    public long getExpired() { return expired; }
    public long getSuspended() { return suspended; }
    public long getExpiringSoon() { return expiringSoon; }
    public long getChecksToday() { return checksToday; }
    public long getFailedChecksToday() { return failedChecksToday; }
    public List<NameCount> getByCampus() { return byCampus; }
    public List<NameCount> getRegistrationsByMonth() { return registrationsByMonth; }

    public int getActivePercent() {
        return total == 0 ? 0 : (int) Math.round(active * 100.0 / total);
    }
}
