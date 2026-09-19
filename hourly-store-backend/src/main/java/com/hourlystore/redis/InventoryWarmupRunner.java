package com.hourlystore.redis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hourlystore.entity.Coupon;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.mapper.CouponMapper;
import com.hourlystore.mapper.FlashSaleEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Warm Redis stock mirrors from MySQL so Lua paths have initial values after deploy/restart.
 * Flash-sale and coupon warmups are independent so a missing DB column does not block coupon keys.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class InventoryWarmupRunner implements ApplicationRunner {

    private final FlashSaleEventMapper flashSaleEventMapper;
    private final CouponMapper couponMapper;
    private final AtomicStockRedisService atomicStockRedisService;

    @Override
    public void run(ApplicationArguments args) {
        int flashCount = 0;
        try {
            List<FlashSaleEvent> events = flashSaleEventMapper.selectList(new LambdaQueryWrapper<>());
            for (FlashSaleEvent e : events) {
                if (e.getId() != null && e.getStock() != null) {
                    atomicStockRedisService.overwriteFlashStock(e.getId(), e.getStock());
                    flashCount++;
                }
            }
            log.info("Redis flash-sale inventory mirrors warmed: {} events", flashCount);
        } catch (Exception ex) {
            log.warn(
                    "Flash-sale inventory warmup skipped (run migration_add_product.sql if column product_id is missing): {}",
                    ex.getMessage());
        }

        int couponCount = 0;
        try {
            List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<>());
            for (Coupon c : coupons) {
                if (c.getId() != null && c.getStockRemain() != null) {
                    atomicStockRedisService.overwriteCouponStock(c.getId(), c.getStockRemain());
                    couponCount++;
                }
            }
            log.info("Redis coupon inventory mirrors warmed: {} coupons", couponCount);
        } catch (Exception ex) {
            log.warn("Coupon inventory warmup skipped: {}", ex.getMessage());
        }
    }
}
