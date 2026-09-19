-- Run on existing hourlystore DB: mysql -u root -p hourlystore < migration_full_requirements.sql
USE hourlystore;

ALTER TABLE `user`
    ADD COLUMN email VARCHAR(128) DEFAULT NULL AFTER phone,
    ADD COLUMN role TINYINT NOT NULL DEFAULT 0 COMMENT '0=user 1=admin' AFTER status;

ALTER TABLE `user`
    MODIFY phone VARCHAR(32) NULL;

ALTER TABLE `user`
    ADD UNIQUE KEY uk_user_email (email);

-- Demo admin (password: admin123 — change after first login in production)
-- BCrypt hash for "admin123"
INSERT INTO `user` (phone, email, password_hash, nickname, status, role)
SELECT '13800000000', 'admin@hourlystore.local',
       '$2a$10$oFdiiHBbQsXfmX.M6sPhk.3rcSkNCevbYp/i0g2rpyFNyi/ZGnRzS',
       'Admin', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE role = 1);
