package tut.ac.za.dsc.service;

/** A label with a count and its share of the largest count (for drawing bars). */
public class NameCount {

    private final String name;
    private final long count;
    private int percent;

    public NameCount(String name, long count) {
        this.name = name == null || name.isBlank() ? "Not set" : name;
        this.count = count;
    }

    public String getName() { return name; }
    public long getCount() { return count; }
    public int getPercent() { return percent; }
    void setPercent(int percent) { this.percent = percent; }
}
