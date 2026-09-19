-- Add merchant service description (R11) for existing databases.
USE hourlystore;

ALTER TABLE merchant
    ADD COLUMN service_description VARCHAR(2048) DEFAULT NULL
        COMMENT 'Merchant service scope / offerings' AFTER address;

UPDATE merchant SET service_description = 'Full-service dining: lunch sets, dine-in and takeaway.' WHERE name = 'Demo Restaurant' AND service_description IS NULL;
UPDATE merchant SET service_description = 'General retail: daily goods, flash-sale friendly SKUs.' WHERE name = 'Demo Shop' AND service_description IS NULL;
