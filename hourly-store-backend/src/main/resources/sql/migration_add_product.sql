-- Run on existing HourlyStore DBs that were created before the product feature (MySQL 8).
USE hourlystore;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024) DEFAULT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    image_url VARCHAR(512) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE flash_sale_event
    ADD COLUMN product_id BIGINT DEFAULT NULL AFTER coupon_id,
    ADD CONSTRAINT fk_flash_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE SET NULL;

ALTER TABLE `order`
    ADD COLUMN product_id BIGINT DEFAULT NULL AFTER merchant_id,
    ADD CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE SET NULL;
