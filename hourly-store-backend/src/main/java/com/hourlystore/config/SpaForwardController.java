package com.hourlystore.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue Router history 模式：直接访问 /merchants 等路径时转发到 index.html。
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/login",
            "/register",
            "/merchants",
            "/products",
            "/flash-sales",
            "/profile",
            "/admin/flash-sales"
    })
    public String spaFlat() {
        return "forward:/index.html";
    }

    @GetMapping("/merchants/{id:\\d+}")
    public String spaMerchantDetail() {
        return "forward:/index.html";
    }

    @GetMapping("/products/{id:\\d+}")
    public String spaProductDetail() {
        return "forward:/index.html";
    }
}
