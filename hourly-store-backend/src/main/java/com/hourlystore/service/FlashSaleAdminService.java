package com.hourlystore.service;

import com.hourlystore.common.BizException;
import com.hourlystore.dto.CreateFlashSaleRequest;
import com.hourlystore.dto.UpdateFlashSaleRequest;
import com.hourlystore.entity.Coupon;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.mapper.CouponMapper;
import com.hourlystore.mapper.FlashSaleEventMapper;
import com.hourlystore.mapper.ProductMapper;
import com.hourlystore.redis.AtomicStockRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlashSaleAdminService {

    private final FlashSaleEventMapper flashSaleEventMapper;
    private final CouponMapper couponMapper;
    private final ProductMapper productMapper;
    private final AtomicStockRedisService atomicStockRedisService;
    private final FlashSaleService flashSaleService;

    @Transactional
    public FlashSaleEvent create(CreateFlashSaleRequest req) {
        Coupon coupon = couponMapper.selectById(req.getCouponId());
        if (coupon == null) {
            throw new BizException("Coupon not found");
        }
        if (req.getEndTime().isBefore(req.getBeginTime())) {
            throw new BizException("End time must be after begin time");
        }
        Long productId = resolveProductId(req.getProductId());
        FlashSaleEvent event = new FlashSaleEvent();
        event.setCouponId(req.getCouponId());
        event.setProductId(productId);
        event.setTitle(req.getTitle());
        event.setStock(req.getStock());
        event.setBeginTime(req.getBeginTime());
        event.setEndTime(req.getEndTime());
        event.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        flashSaleEventMapper.insert(event);
        atomicStockRedisService.overwriteFlashStock(event.getId(), event.getStock() == null ? 0 : event.getStock());
        flashSaleService.evictActiveListCache();
        return flashSaleEventMapper.selectById(event.getId());
    }

    @Transactional
    public FlashSaleEvent update(Long id, UpdateFlashSaleRequest req) {
        FlashSaleEvent event = flashSaleEventMapper.selectById(id);
        if (event == null) {
            throw new BizException("Flash sale event not found");
        }
        if (req.getCouponId() != null) {
            if (couponMapper.selectById(req.getCouponId()) == null) {
                throw new BizException("Coupon not found");
            }
            event.setCouponId(req.getCouponId());
        }
        if (req.getProductId() != null) {
            event.setProductId(resolveProductId(req.getProductId()));
        }
        if (req.getTitle() != null) {
            event.setTitle(req.getTitle());
        }
        if (req.getStock() != null) {
            event.setStock(req.getStock());
        }
        if (req.getBeginTime() != null) {
            event.setBeginTime(req.getBeginTime());
        }
        if (req.getEndTime() != null) {
            event.setEndTime(req.getEndTime());
        }
        if (req.getStatus() != null) {
            event.setStatus(req.getStatus());
        }
        if (event.getEndTime().isBefore(event.getBeginTime())) {
            throw new BizException("End time must be after begin time");
        }
        flashSaleEventMapper.updateById(event);
        if (req.getStock() != null) {
            atomicStockRedisService.overwriteFlashStock(id, event.getStock());
        }
        flashSaleService.evictActiveListCache();
        return flashSaleEventMapper.selectById(id);
    }

    private Long resolveProductId(Long productId) {
        if (productId == null) {
            return null;
        }
        if (productMapper.selectById(productId) == null) {
            throw new BizException("Product not found: " + productId);
        }
        return productId;
    }
}
