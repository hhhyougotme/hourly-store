package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hourlystore.common.BizException;
import com.hourlystore.cache.DelayedDoubleDeleteEvictor;
import com.hourlystore.dto.PlaceOrderResponse;
import com.hourlystore.entity.Coupon;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.entity.OrderEntity;
import com.hourlystore.entity.Product;
import com.hourlystore.mapper.CouponMapper;
import com.hourlystore.mapper.FlashSaleEventMapper;
import com.hourlystore.mapper.OrderMapper;
import com.hourlystore.mapper.ProductMapper;
import com.hourlystore.redis.AtomicStockRedisService;
import com.hourlystore.redis.OrderStreamPublisher;
import com.hourlystore.redis.StreamFeatureGate;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final FlashSaleEventMapper flashSaleEventMapper;
    private final CouponMapper couponMapper;
    private final OrderMapper orderMapper;
    private final AtomicStockRedisService atomicStockRedisService;
    private final OrderStreamPublisher orderStreamPublisher;
    private final RedissonClient redissonClient;
    private final FlashSaleService flashSaleService;
    private final DelayedDoubleDeleteEvictor delayedDoubleDeleteEvictor;
    private final StreamFeatureGate streamFeatureGate;
    private final OrderPersistenceService orderPersistenceService;
    private final OrderResultService orderResultService;
    private final ProductMapper productMapper;

    @Value("${hourlystore.streams.order-wait-ms:5000}")
    private long orderWaitMs;

    public PlaceOrderResponse placeFlashSaleOrder(Long userId, Long flashSaleEventId, BigDecimal amount) {
        RLock lock = redissonClient.getLock("hourlystore:lock:flash-order:" + flashSaleEventId);
        boolean locked;
        try {
            locked = lock.tryLock(3, 25, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("Interrupted while acquiring lock");
        }
        if (!locked) {
            throw new BizException("System busy, try again");
        }
        try {
            ReservationContext ctx = reserveStock(userId, flashSaleEventId, amount);
            if (streamFeatureGate.isEnabled()) {
                return placeViaStream(ctx);
            }
            return placeSynchronously(ctx);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PlaceOrderResponse placeViaStream(ReservationContext ctx) {
        String correlationId = UUID.randomUUID().toString().replace("-", "");
        orderStreamPublisher.publishCreateOrder(
                correlationId,
                ctx.userId,
                ctx.eventId,
                ctx.merchantId,
                ctx.productId,
                ctx.amount);
        try {
            PlaceOrderResponse polled = orderResultService.poll(correlationId, orderWaitMs);
            if (polled != null) {
                if ("SUCCESS".equals(polled.getStatus())) {
                    delayedDoubleDeleteEvictor.scheduleSecondDelete(flashSaleService::evictActiveListCache);
                    return polled;
                }
                rollbackReservation(ctx);
                return polled;
            }
            rollbackReservation(ctx);
            return PlaceOrderResponse.failed("Order processing timeout, please retry");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rollbackReservation(ctx);
            return PlaceOrderResponse.failed("Order processing interrupted");
        }
    }

    private PlaceOrderResponse placeSynchronously(ReservationContext ctx) {
        try {
            OrderEntity order = orderPersistenceService.insertOrderSync(
                    ctx.userId, ctx.eventId, ctx.merchantId, ctx.productId, ctx.amount);
            delayedDoubleDeleteEvictor.scheduleSecondDelete(flashSaleService::evictActiveListCache);
            return PlaceOrderResponse.success(order);
        } catch (BizException e) {
            rollbackReservation(ctx);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected ReservationContext reserveStock(Long userId, Long flashSaleEventId, BigDecimal amount) {
        FlashSaleEvent event = flashSaleEventMapper.selectById(flashSaleEventId);
        if (event == null) {
            throw new BizException("Flash sale event not found");
        }
        if (event.getStatus() == null || event.getStatus() != 1) {
            throw new BizException("Event not active");
        }
        var now = java.time.LocalDateTime.now();
        if (now.isBefore(event.getBeginTime()) || now.isAfter(event.getEndTime())) {
            throw new BizException("Event not in time window");
        }
        Coupon coupon = couponMapper.selectById(event.getCouponId());
        if (coupon == null) {
            throw new BizException("Coupon for event not found");
        }

        atomicStockRedisService.setFlashStockIfAbsent(flashSaleEventId, event.getStock() == null ? 0 : event.getStock());
        long lua = atomicStockRedisService.tryDecrementFlash(flashSaleEventId);
        if (lua == 0) {
            throw new BizException("Flash sale sold out");
        }
        if (lua < 0) {
            atomicStockRedisService.overwriteFlashStock(flashSaleEventId, event.getStock() == null ? 0 : event.getStock());
            lua = atomicStockRedisService.tryDecrementFlash(flashSaleEventId);
            if (lua != 1) {
                throw new BizException("Flash sale sold out");
            }
        }

        delayedDoubleDeleteEvictor.firstDelete(flashSaleService::evictActiveListCache);

        int rows = flashSaleEventMapper.update(
                null,
                new LambdaUpdateWrapper<FlashSaleEvent>()
                        .eq(FlashSaleEvent::getId, flashSaleEventId)
                        .gt(FlashSaleEvent::getStock, 0)
                        .setSql("stock = stock - 1"));
        if (rows == 0) {
            atomicStockRedisService.incrementFlash(flashSaleEventId);
            throw new BizException("Flash sale sold out");
        }

        FlashSaleEvent fresh = flashSaleEventMapper.selectById(flashSaleEventId);
        if (fresh != null && fresh.getStock() != null) {
            atomicStockRedisService.overwriteFlashStock(flashSaleEventId, fresh.getStock());
        }

        ReservationContext ctx = new ReservationContext();
        ctx.userId = userId;
        ctx.eventId = flashSaleEventId;
        ctx.merchantId = coupon.getMerchantId();
        ctx.productId = event.getProductId();
        ctx.amount = amount != null ? amount : BigDecimal.ONE;
        return ctx;
    }

    private void rollbackReservation(ReservationContext ctx) {
        atomicStockRedisService.incrementFlash(ctx.eventId);
        flashSaleEventMapper.update(
                null,
                new LambdaUpdateWrapper<FlashSaleEvent>()
                        .eq(FlashSaleEvent::getId, ctx.eventId)
                        .setSql("stock = stock + 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PlaceOrderResponse placeProductOrder(Long userId, Long productId, Integer quantity) {
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        RLock lock = redissonClient.getLock("hourlystore:lock:product-order:" + productId);
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
            Product product = productMapper.selectById(productId);
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BizException("Product not found or off shelf");
            }
            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock < qty) {
                throw new BizException("Insufficient stock");
            }
            int rows = productMapper.update(
                    null,
                    new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, productId)
                            .ge(Product::getStock, qty)
                            .setSql("stock = stock - " + qty));
            if (rows == 0) {
                throw new BizException("Insufficient stock");
            }
            BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(qty));
            OrderEntity order = new OrderEntity();
            order.setUserId(userId);
            order.setMerchantId(product.getMerchantId());
            order.setProductId(productId);
            order.setFlashSaleEventId(null);
            order.setAmount(amount);
            order.setStatus(1);
            orderMapper.insert(order);
            return PlaceOrderResponse.success(orderMapper.selectById(order.getId()));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public java.util.List<OrderEntity> listMine(Long userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getUserId, userId)
                        .orderByDesc(OrderEntity::getId));
    }

    private static class ReservationContext {
        long userId;
        long eventId;
        long merchantId;
        Long productId;
        BigDecimal amount;
    }
}
