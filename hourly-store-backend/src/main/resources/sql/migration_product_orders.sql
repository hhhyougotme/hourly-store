-- Allow regular product orders (flash_sale_event_id nullable).
USE hourlystore;

ALTER TABLE `order`
    MODIFY flash_sale_event_id BIGINT DEFAULT NULL COMMENT 'NULL for product orders';
