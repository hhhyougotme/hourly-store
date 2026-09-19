package com.hourlystore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hourlystore.common.BizException;
import com.hourlystore.dto.CreateProductRequest;
import com.hourlystore.entity.Merchant;
import com.hourlystore.entity.Product;
import com.hourlystore.mapper.MerchantMapper;
import com.hourlystore.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;

    public List<Product> listOnShelfByMerchant(Long merchantId) {
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantId, merchantId)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getId));
    }

    public Product getOnShelfById(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null || p.getStatus() == null || p.getStatus() != 1) {
            throw new BizException("Product not found");
        }
        return p;
    }

    @Transactional(rollbackFor = Exception.class)
    public Product create(CreateProductRequest req) {
        Merchant m = merchantMapper.selectById(req.getMerchantId());
        if (m == null) {
            throw new BizException("Merchant not found");
        }
        Product p = new Product();
        p.setMerchantId(req.getMerchantId());
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock() != null ? req.getStock() : 0);
        p.setStatus(1);
        p.setImageUrl(req.getImageUrl());
        productMapper.insert(p);
        return productMapper.selectById(p.getId());
    }

    /** For enrichment; returns null if missing or off-shelf. */
    public Product findByIdForDisplay(Long id) {
        if (id == null) {
            return null;
        }
        Product p = productMapper.selectById(id);
        if (p == null || p.getStatus() == null || p.getStatus() != 1) {
            return null;
        }
        return p;
    }
}
