package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourlystore.common.BizException;
import com.hourlystore.dto.PageResult;
import com.hourlystore.entity.Merchant;
import com.hourlystore.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private static final int LIST_TTL_BASE_SEC = 180;
    private static final int LIST_TTL_JITTER_SEC = 120;
    private static final int ONE_TTL_BASE_SEC = 120;
    private static final int ONE_TTL_JITTER_SEC = 120;
    private static final int MAX_PAGE_SIZE = 50;
    private final MerchantMapper merchantMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    public PageResult<Merchant> listPage(Long merchantTypeId, int page, int pageSize) {
        long p = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        String key = listPageKey(merchantTypeId, p, size);

        PageResult<Merchant> cached = readPageCache(key);
        if (cached != null) {
            return cached;
        }

        String lockKey = "hourlystore:lock:cache-rebuild:" + key;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 8, TimeUnit.SECONDS);
            if (locked) {
                cached = readPageCache(key);
                if (cached != null) {
                    return cached;
                }
                PageResult<Merchant> fromDb = loadPageFromDb(merchantTypeId, p, size);
                writePageCache(key, fromDb);
                return fromDb;
            }
            cached = readPageCache(key);
            if (cached != null) {
                return cached;
            }
            log.debug("Merchant list cache rebuild lock busy, loading page from DB: {}", key);
            return loadPageFromDb(merchantTypeId, p, size);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loadPageFromDb(merchantTypeId, p, size);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Merchant getById(Long id) {
        String key = detailKey(id);
        Merchant cached = readDetailCache(key);
        if (cached != null) {
            return cached;
        }

        String lockKey = "hourlystore:lock:cache-rebuild:" + key;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 8, TimeUnit.SECONDS);
            if (locked) {
                cached = readDetailCache(key);
                if (cached != null) {
                    return cached;
                }
                Merchant fromDb = loadDetailFromDb(id);
                writeDetailCache(key, fromDb);
                return fromDb;
            }
            cached = readDetailCache(key);
            if (cached != null) {
                return cached;
            }
            return loadDetailFromDb(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loadDetailFromDb(id);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PageResult<Merchant> loadPageFromDb(Long merchantTypeId, long page, long pageSize) {
        LambdaQueryWrapper<Merchant> w = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 1)
                .orderByDesc(Merchant::getId);
        if (merchantTypeId != null) {
            w.eq(Merchant::getMerchantTypeId, merchantTypeId);
        }
        Page<Merchant> mp = merchantMapper.selectPage(new Page<>(page, pageSize), w);
        return PageResult.of(mp.getRecords(), mp.getTotal(), page, pageSize);
    }

    private Merchant loadDetailFromDb(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null || (m.getStatus() != null && m.getStatus() != 1)) {
            throw new BizException("Merchant not found");
        }
        return m;
    }

    private PageResult<Merchant> readPageCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<PageResult<Merchant>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private Merchant readDetailCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, Merchant.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writePageCache(String key, PageResult<Merchant> page) {
        try {
            int ttl = LIST_TTL_BASE_SEC + ThreadLocalRandom.current().nextInt(LIST_TTL_JITTER_SEC + 1);
            String payload = objectMapper.writeValueAsString(page);
            stringRedisTemplate.opsForValue().set(key, payload, Duration.ofSeconds(ttl));
        } catch (Exception ignored) {
        }
    }

    private void writeDetailCache(String key, Merchant merchant) {
        try {
            int ttl = ONE_TTL_BASE_SEC + ThreadLocalRandom.current().nextInt(ONE_TTL_JITTER_SEC + 1);
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(merchant), Duration.ofSeconds(ttl));
        } catch (Exception ignored) {
        }
    }

    private static String listPageKey(Long merchantTypeId, long page, long pageSize) {
        String type = merchantTypeId == null ? "all" : String.valueOf(merchantTypeId);
        return "hourlystore:cache:merchants:page:" + type + ":" + page + ":" + pageSize;
    }

    private static String detailKey(Long id) {
        return "hourlystore:cache:merchant:" + id;
    }
}
