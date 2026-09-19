package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hourlystore.common.BizException;
import com.hourlystore.entity.Coupon;
import com.hourlystore.entity.CouponClaim;
import com.hourlystore.mapper.CouponClaimMapper;
import com.hourlystore.mapper.CouponMapper;
import com.hourlystore.cache.DelayedDoubleDeleteEvictor;
import com.hourlystore.redis.AtomicStockRedisService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponClaimService {

    private final CouponMapper couponMapper;
    private final CouponClaimMapper couponClaimMapper;
    private final RedissonClient redissonClient;
    private final AtomicStockRedisService atomicStockRedisService;
    private final CouponService couponService;
    private final DelayedDoubleDeleteEvictor delayedDoubleDeleteEvictor;

    @Transactional(rollbackFor = Exception.class)
    public CouponClaim claim(Long userId, Long couponId) {
        RLock lock = redissonClient.getLock("hourlystore:lock:coupon-claim:" + couponId);
        boolean locked;
        try {
            locked = lock.tryLock(3, 20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("Interrupted while acquiring lock");
        }
        if (!locked) {
            throw new BizException("System busy, try again");
        }
        try {
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null) {
                throw new BizException("Coupon not found");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(coupon.getBeginTime()) || now.isAfter(coupon.getEndTime())) {
                throw new BizException("Coupon not in valid time window");
            }

            atomicStockRedisService.setCouponStockIfAbsent(couponId, coupon.getStockRemain() == null ? 0 : coupon.getStockRemain());
            long lua = atomicStockRedisService.tryDecrementCoupon(couponId);
            if (lua == 0) {
                throw new BizException("Coupon out of stock");
            }
            if (lua < 0) {
                atomicStockRedisService.overwriteCouponStock(couponId, coupon.getStockRemain() == null ? 0 : coupon.getStockRemain());
                lua = atomicStockRedisService.tryDecrementCoupon(couponId);
                if (lua != 1) {
                    throw new BizException("Coupon out of stock");
                }
            }

            delayedDoubleDeleteEvictor.firstDelete(() -> couponService.evictMerchantCouponCaches(coupon.getMerchantId()));

            int rows = couponMapper.update(
                    null,
                    new LambdaUpdateWrapper<Coupon>()
                            .eq(Coupon::getId, couponId)
                            .gt(Coupon::getStockRemain, 0)
                            .setSql("stock_remain = stock_remain - 1"));
            if (rows == 0) {
                atomicStockRedisService.incrementCoupon(couponId);
                throw new BizException("Coupon out of stock");
            }

            CouponClaim claim = new CouponClaim();
            claim.setUserId(userId);
            claim.setCouponId(couponId);
            claim.setClaimedAt(now);
            claim.setStatus(1);
            try {
                couponClaimMapper.insert(claim);
            } catch (DuplicateKeyException e) {
                atomicStockRedisService.incrementCoupon(couponId);
                couponMapper.update(
                        null,
                        new LambdaUpdateWrapper<Coupon>()
                                .eq(Coupon::getId, couponId)
                                .setSql("stock_remain = stock_remain + 1"));
                throw new BizException("Already claimed this coupon");
            }

            Coupon fresh = couponMapper.selectById(couponId);
            if (fresh != null && fresh.getStockRemain() != null) {
                atomicStockRedisService.overwriteCouponStock(couponId, fresh.getStockRemain());
            }
            delayedDoubleDeleteEvictor.scheduleSecondDelete(() -> couponService.evictMerchantCouponCaches(coupon.getMerchantId()));
            return claim;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public java.util.List<CouponClaim> listMine(Long userId) {
        return couponClaimMapper.selectList(
                new LambdaQueryWrapper<CouponClaim>()
                        .eq(CouponClaim::getUserId, userId)
                        .orderByDesc(CouponClaim::getId));
    }
}
