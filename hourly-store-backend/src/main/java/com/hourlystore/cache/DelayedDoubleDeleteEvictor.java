package com.hourlystore.cache;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 延迟双删：先删缓存，调用方完成数据库更新后，再在一段时间后第二次删除，
 * 降低并发下「读库旧数据又回写缓存」导致脏读的概率。
 */
@Component
public class DelayedDoubleDeleteEvictor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hourlystore-delayed-cache-evict");
        t.setDaemon(true);
        return t;
    });

    @Value("${hourlystore.cache.double-delete-delay-ms:400}")
    private long delayMs;

    /** 写库前第一次删缓存（与 {@link #scheduleSecondDelete(Runnable)} 成对使用）。 */
    public void firstDelete(Runnable evict) {
        try {
            evict.run();
        } catch (Exception ignored) {
        }
    }

    /** 写库成功后调度延迟第二删，降低并发回写脏缓存窗口。 */
    public void scheduleSecondDelete(Runnable evict) {
        long d = Math.max(0L, delayMs);
        scheduler.schedule(() -> {
            try {
                evict.run();
            } catch (Exception ignored) {
            }
        }, d, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
