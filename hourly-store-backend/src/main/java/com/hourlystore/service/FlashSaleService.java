package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourlystore.common.BizException;
import com.hourlystore.dto.FlashSaleListItemDto;
import com.hourlystore.entity.FlashSaleEvent;
import com.hourlystore.entity.Product;
import com.hourlystore.mapper.FlashSaleEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private static final String ACTIVE_KEY = "hourlystore:cache:flash-sales:active-dtos";
    private static final int TTL_BASE_SEC = 60;
    private static final int TTL_JITTER_SEC = 60;

    private final FlashSaleEventMapper flashSaleEventMapper;
    private final ProductService productService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<FlashSaleListItemDto> listActive() {
        try {
            String json = stringRedisTemplate.opsForValue().get(ACTIVE_KEY);
            if (json != null) {
                if (json.isEmpty()) {
                    return List.of();
                }
                return objectMapper.readValue(json, new TypeReference<List<FlashSaleListItemDto>>() {});
            }
        } catch (Exception ignored) {
        }

        var now = java.time.LocalDateTime.now();
        List<FlashSaleEvent> events = flashSaleEventMapper.selectList(
                new LambdaQueryWrapper<FlashSaleEvent>()
                        .eq(FlashSaleEvent::getStatus, 1)
                        .le(FlashSaleEvent::getBeginTime, now)
                        .ge(FlashSaleEvent::getEndTime, now)
                        .orderByDesc(FlashSaleEvent::getId));
        List<FlashSaleListItemDto> dtos = events.stream().map(this::toListItem).collect(Collectors.toList());
        try {
            int ttl = TTL_BASE_SEC + ThreadLocalRandom.current().nextInt(TTL_JITTER_SEC + 1);
            String payload = dtos.isEmpty() ? "" : objectMapper.writeValueAsString(dtos);
            stringRedisTemplate.opsForValue().set(ACTIVE_KEY, payload, Duration.ofSeconds(ttl));
        } catch (Exception ignored) {
        }
        return dtos;
    }

    public void evictActiveListCache() {
        stringRedisTemplate.delete(ACTIVE_KEY);
    }

    private FlashSaleListItemDto toListItem(FlashSaleEvent e) {
        FlashSaleListItemDto d = new FlashSaleListItemDto();
        d.setId(e.getId());
        d.setCouponId(e.getCouponId());
        d.setProductId(e.getProductId());
        d.setTitle(e.getTitle());
        d.setStock(e.getStock());
        d.setBeginTime(e.getBeginTime());
        d.setEndTime(e.getEndTime());
        d.setStatus(e.getStatus());
        Product p = productService.findByIdForDisplay(e.getProductId());
        if (p != null) {
            d.setProductName(p.getName());
            d.setProductPrice(p.getPrice());
        }
        return d;
    }

    public FlashSaleEvent requireById(Long id) {
        FlashSaleEvent e = flashSaleEventMapper.selectById(id);
        if (e == null) {
            throw new BizException("Flash sale event not found");
        }
        return e;
    }
}
