package vn.simtim.api.sale.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.simtim.api.sale.application.TransferService;

@Configuration
@EnableScheduling
class TransferExpiryTask {
    private static final Logger LOG=LoggerFactory.getLogger(TransferExpiryTask.class);
    private final TransferService service;
    TransferExpiryTask(TransferService service) { this.service=service; }
    @Scheduled(fixedDelayString="${TRANSFER_EXPIRY_INTERVAL_MS:30000}")
    public void expire() {
        for (var id:service.due()) {
            try { service.expire(id); }
            catch (RuntimeException failure) { LOG.warn("Cannot release expired transfer {}: {}",id,failure.getClass().getSimpleName()); }
        }
    }
}
