package tut.ac.za.dsc.service;

import java.util.List;
import org.springframework.data.domain.Page;

/** One page of results with the numbers the pager needs (pages counted from 1). */
public class PageView<T> {

    private final List<T> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public PageView(Page<T> p) {
        this.items = p.getContent();
        this.total = p.getTotalElements();
        this.page = p.getNumber() + 1;
        this.pageSize = p.getSize();
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageCount() { return (int) Math.max(1, (total + pageSize - 1) / pageSize); }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < getPageCount(); }
    public int getPreviousPage() { return page - 1; }
    public int getNextPage() { return page + 1; }
    public long getFirstShown() { return total == 0 ? 0 : (long) (page - 1) * pageSize + 1; }
    public long getLastShown() { return Math.min(total, (long) page * pageSize); }
}
