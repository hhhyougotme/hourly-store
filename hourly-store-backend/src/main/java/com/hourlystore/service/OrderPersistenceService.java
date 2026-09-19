package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hourlystore.common.BizException;
import com.hourlystore.dto.PlaceOrderResponse;
import com.hourlystore.entity.Coupon;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.entity.OrderEntity;
import com.hourlystore.mapper.CouponMapper;
import com.hourlystore.mapper.FlashSaleEventMapper;
import com.hourlystore.mapper.OrderMapper;
import com.hourlystore.redis.AtomicStockRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderMapper orderMapper;
    private final FlashSaleEventMapper flashSaleEventMapper;
    private final CouponMapper couponMapper;
    private final AtomicStockRedisService atomicStockRedisService;
    private final OrderResultService orderResultService;

    @Transactional(rollbackFor = Exception.class)
    public void persistFromStreamMessage(Map<String, String> fields) {
        String correlationId = fields.get("correlationId");
        if (correlationId == null) {
            return;
        }
        try {
            long userId = Long.parseLong(fields.get("userId"));
            long flashSaleEventId = Long.parseLong(fields.get("flashSaleEventId"));
            long merchantId = Long.parseLong(fields.get("merchantId"));
            Long productId = parseLongOrNull(fields.get("productId"));
            BigDecimal amount = new BigDecimal(fields.getOrDefault("amount", "1"));

            OrderEntity order = new OrderEntity();
            order.setUserId(userId);
            order.setMerchantId(merchantId);
            order.setProductId(productId);
            order.setFlashSaleEventId(flashSaleEventId);
            order.setAmount(amount);
            order.setStatus(1);
            orderMapper.insert(order);

            OrderEntity saved = orderMapper.selectById(order.getId());
            orderResultService.saveSuccess(correlationId, PlaceOrderResponse.success(saved));
        } catch (DuplicateKeyException e) {
            orderResultService.saveFailed(correlationId, "Already ordered for this event");
        } catch (Exception e) {
            rollbackReservation(fields);
            orderResultService.saveFailed(correlationId, e.getMessage() != null ? e.getMessage() : "Order failed");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderEntity insertOrderSync(long userId, long flashSaleEventId, long merchantId, Long productId, BigDecimal amount) {
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setProductId(productId);
        order.setFlashSaleEventId(flashSaleEventId);
        order.setAmount(amount != null ? amount : BigDecimal.ONE);
        order.setStatus(1);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BizException("Already ordered for this event");
        }
        return orderMapper.selectById(order.getId());
    }

    private void rollbackReservation(Map<String, String> fields) {
        try {
            long flashSaleEventId = Long.parseLong(fields.get("flashSaleEventId"));
            atomicStockRedisService.incrementFlash(flashSaleEventId);
            flashSaleEventMapper.update(
                    null,
                    new LambdaUpdateWrapper<FlashSaleEvent>()
                            .eq(FlashSaleEvent::getId, flashSaleEventId)
                            .setSql("stock = stock + 1"));
        } catch (Exception ignored) {
        }
    }

    private static Long parseLongOrNull(String v) {
        if (v == null || v.isBlank() || "null".equals(v)) {
            return null;
        }
        return Long.parseLong(v);
    }
}
