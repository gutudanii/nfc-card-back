package com.toollix.analytics.worker;

import com.toollix.analytics.repo.AnalyticsEventRepository;
import com.toollix.analytics.model.AnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsWorker {
    private final Logger log = LoggerFactory.getLogger(AnalyticsWorker.class);
    private final AnalyticsEventRepository repo;

    public AnalyticsWorker(AnalyticsEventRepository repo) {
        this.repo = repo;
    }

    @Async
    public void record(AnalyticsEvent e) {
        log.info("Recording analytics event async: {}", e.getEventType());
        repo.save(e);
    }
}
