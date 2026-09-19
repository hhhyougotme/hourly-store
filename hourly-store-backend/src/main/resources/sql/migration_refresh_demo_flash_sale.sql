-- Extend demo flash-sale window on existing databases (safe to re-run).
USE hourlystore;

UPDATE flash_sale_event
SET begin_time = NOW(),
    end_time = DATE_ADD(NOW(), INTERVAL 30 DAY),
    status = 1,
    stock = GREATEST(stock, 10)
WHERE id = 1;

UPDATE coupon SET stock_remain = GREATEST(stock_remain, 10) WHERE id = 2;
