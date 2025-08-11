-- Add missing columns to orders table
-- This migration adds shipping_address_id column that is referenced in the Order entity

-- Add shipping_address_id column to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_id BIGINT REFERENCES addresses(id);

-- Update existing orders to have a default shipping address if needed
-- (This is optional and can be removed if not needed)
-- UPDATE orders SET shipping_address_id = (SELECT id FROM addresses LIMIT 1) WHERE shipping_address_id IS NULL; 