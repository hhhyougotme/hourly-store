-- Resequence order.id to 1..N after test data deletes (resets AUTO_INCREMENT).
USE hourlystore;

SET FOREIGN_KEY_CHECKS = 0;

UPDATE `order` SET id = -id;

SET @row := 0;
UPDATE `order` SET id = (@row := @row + 1) ORDER BY id DESC;

SET @next := (SELECT IFNULL(MAX(id), 0) + 1 FROM `order`);
SET @sql := CONCAT('ALTER TABLE `order` AUTO_INCREMENT = ', @next);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
