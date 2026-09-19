package com.hourlystore.dto;

import com.hourlystore.entity.OrderEntity;
import lombok.Data;

@Data
public class PlaceOrderResponse {
    /** SUCCESS, FAILED */
    private String status;
    private String message;
    private OrderEntity order;

    public static PlaceOrderResponse success(OrderEntity order) {
        PlaceOrderResponse r = new PlaceOrderResponse();
        r.setStatus("SUCCESS");
        r.setMessage("Order placed successfully");
        r.setOrder(order);
        return r;
    }

    public static PlaceOrderResponse failed(String message) {
        PlaceOrderResponse r = new PlaceOrderResponse();
        r.setStatus("FAILED");
        r.setMessage(message);
        return r;
    }
}
