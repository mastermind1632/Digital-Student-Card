package tut.ac.za.dsc.service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tut.ac.za.dsc.domain.AuditEvent;
import tut.ac.za.dsc.repository.AuditEventRepository;

/** Records and lists who did what. */
@Service
public class AuditService {

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void log(String actor, String action, String target, String details) {
        events.save(new AuditEvent(actor, action, target, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recent(int max) {
        return events.findAllByOrderByIdDesc(PageRequest.of(0, max));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> forTarget(String target, int max) {
        return events.findByTargetOrderByIdDesc(target, PageRequest.of(0, max));
    }

    @Transactional(readOnly = true)
    public PageView<AuditEvent> search(String text, int page, int size) {
        String like = "%" + (text == null ? "" : text.trim().toLowerCase()) + "%";
        return new PageView<>(events.search(like, PageRequest.of(Math.max(page, 1) - 1, size)));
    }
}
