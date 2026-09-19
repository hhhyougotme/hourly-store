-- Fix demo admin password hash (plain password: admin123)
USE hourlystore;

UPDATE `user`
SET password_hash = '$2a$10$oFdiiHBbQsXfmX.M6sPhk.3rcSkNCevbYp/i0g2rpyFNyi/ZGnRzS'
WHERE phone = '13800000000' OR role = 1;
