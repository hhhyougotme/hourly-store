-- HourlyStore — MySQL 8 schema (run once: mysql -u root -p < schema.sql)
CREATE DATABASE IF NOT EXISTS hourlystore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hourlystore;

DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS coupon_claim;
DROP TABLE IF EXISTS flash_sale_event;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS merchant_type;
DROP TABLE IF EXISTS `user`;

CREATE TABLE merchant_type (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    icon VARCHAR(255) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_type_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(512) DEFAULT NULL,
    service_description VARCHAR(2048) DEFAULT NULL,
    score DECIMAL(3,2) DEFAULT 0.00,
    average_price INT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_type FOREIGN KEY (merchant_type_id) REFERENCES merchant_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
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

CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(32) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0=user 1=admin',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_phone (phone),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    stock_total INT NOT NULL DEFAULT 0,
    stock_remain INT NOT NULL DEFAULT 0,
    begin_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flash_sale_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    title VARCHAR(255) DEFAULT NULL,
    stock INT NOT NULL DEFAULT 0,
    begin_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_flash_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
    CONSTRAINT fk_flash_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon_claim (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_claim_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `order` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    flash_sale_event_id BIGINT DEFAULT NULL COMMENT 'NULL for regular product orders',
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_order_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_order_event FOREIGN KEY (flash_sale_event_id) REFERENCES flash_sale_event (id),
    UNIQUE KEY uk_user_event (user_id, flash_sale_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demo seed (optional)
INSERT INTO merchant_type (name, icon, sort_order) VALUES
    ('Food', NULL, 1),
    ('Retail', NULL, 2);

INSERT INTO merchant (merchant_type_id, name, address, service_description, score, average_price, status) VALUES
    (1, 'Demo Restaurant', '1 Main St', 'Full-service dining: lunch sets, dine-in and takeaway.', 4.50, 50, 1),
    (2, 'Demo Shop', '2 Oak Ave', 'General retail: daily goods, flash-sale friendly SKUs.', 4.20, 100, 1);

INSERT INTO coupon (merchant_id, title, stock_total, stock_remain, begin_time, end_time) VALUES
    (1, 'Welcome 10% off', 1000, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),
    (1, 'Flash lunch coupon', 50, 50, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));

INSERT INTO product (merchant_id, name, description, price, stock, status) VALUES
    (1, '招牌午餐套餐', '含主食+饮品', 28.00, 200, 1),
    (1, '经典汉堡', NULL, 18.00, 50, 1),
    (2, '休闲帽', '均码', 99.00, 30, 1);

INSERT INTO flash_sale_event (coupon_id, product_id, title, stock, begin_time, end_time, status) VALUES
    (2, 1, 'Lunch flash sale', 50, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1);

-- Admin demo: phone 13800000000 / password admin123
INSERT INTO `user` (phone, email, password_hash, nickname, status, role) VALUES
    ('13800000000', 'admin@hourlystore.local', '$2a$10$oFdiiHBbQsXfmX.M6sPhk.3rcSkNCevbYp/i0g2rpyFNyi/ZGnRzS', 'Admin', 1, 1);
