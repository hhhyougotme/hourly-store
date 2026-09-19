-- Seed demo products when product table is empty (safe to re-run).
USE hourlystore;

INSERT INTO product (merchant_id, name, description, price, stock, status)
SELECT 1, '招牌午餐套餐', '含主食+饮品', 28.00, 200, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product LIMIT 1);

INSERT INTO product (merchant_id, name, description, price, stock, status)
SELECT 1, '经典汉堡', NULL, 18.00, 50, 1
FROM DUAL
WHERE (SELECT COUNT(*) FROM product) < 2;

INSERT INTO product (merchant_id, name, description, price, stock, status)
SELECT 2, '休闲帽', '均码', 99.00, 30, 1
FROM DUAL
WHERE (SELECT COUNT(*) FROM product) < 3;

UPDATE flash_sale_event SET product_id = 1 WHERE id = 1 AND product_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM product WHERE id = 1);
