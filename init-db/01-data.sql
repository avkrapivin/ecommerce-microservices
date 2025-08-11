-- Initialize E-commerce Database with Test Data
-- This script creates tables and inserts sample data for local development

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create sequences for auto-incrementing IDs
CREATE SEQUENCE IF NOT EXISTS users_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS categories_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS products_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS orders_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS order_items_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS product_images_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS product_reviews_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS product_specifications_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS product_reservations_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS shipping_info_id_seq START 1;

-- Insert test categories
INSERT INTO categories (id, name, description, parent_id, active, created_at, updated_at) VALUES
(1, 'Electronics', 'Electronic devices and gadgets', NULL, true, NOW(), NOW()),
(2, 'Clothing', 'Men and women clothing', NULL, true, NOW(), NOW()),
(3, 'Books', 'Books and literature', NULL, true, NOW(), NOW()),
(4, 'Home & Garden', 'Home improvement and garden supplies', NULL, true, NOW(), NOW()),
(5, 'Sports', 'Sports equipment and accessories', NULL, true, NOW(), NOW()),
(6, 'Smartphones', 'Mobile phones and accessories', 1, true, NOW(), NOW()),
(7, 'Laptops', 'Portable computers', 1, true, NOW(), NOW()),
(8, 'Men Clothing', 'Clothing for men', 2, true, NOW(), NOW()),
(9, 'Women Clothing', 'Clothing for women', 2, true, NOW(), NOW()),
(10, 'Fiction', 'Fiction books', 3, true, NOW(), NOW()),
(11, 'Non-Fiction', 'Non-fiction books', 3, true, NOW(), NOW());

-- Insert test products
INSERT INTO products (id, name, description, price, stock_quantity, category_id, sku, active, created_at, updated_at) VALUES
(1, 'iPhone 15 Pro', 'Latest Apple smartphone with advanced features', 999.99, 50, 6, 'IPHONE-15-PRO', true, NOW(), NOW()),
(2, 'Samsung Galaxy S24', 'Premium Android smartphone', 899.99, 45, 6, 'SAMSUNG-S24', true, NOW(), NOW()),
(3, 'MacBook Pro 14"', 'Professional laptop for developers and designers', 1999.99, 25, 7, 'MACBOOK-PRO-14', true, NOW(), NOW()),
(4, 'Dell XPS 13', 'Ultrabook with premium design', 1299.99, 30, 7, 'DELL-XPS-13', true, NOW(), NOW()),
(5, 'Men Casual T-Shirt', 'Comfortable cotton t-shirt for everyday wear', 29.99, 100, 8, 'MEN-TSHIRT-001', true, NOW(), NOW()),
(6, 'Women Summer Dress', 'Elegant summer dress for special occasions', 79.99, 60, 9, 'WOMEN-DRESS-001', true, NOW(), NOW()),
(7, 'The Great Gatsby', 'Classic American novel by F. Scott Fitzgerald', 12.99, 200, 10, 'BOOK-GATSBY', true, NOW(), NOW()),
(8, 'Clean Code', 'Programming best practices by Robert C. Martin', 39.99, 75, 11, 'BOOK-CLEAN-CODE', true, NOW(), NOW()),
(9, 'Garden Tool Set', 'Complete set of essential garden tools', 89.99, 40, 4, 'GARDEN-TOOLS', true, NOW(), NOW()),
(10, 'Yoga Mat', 'Premium non-slip yoga mat', 49.99, 80, 5, 'YOGA-MAT-001', true, NOW(), NOW()),
(11, 'Wireless Headphones', 'Noise-cancelling wireless headphones', 199.99, 35, 1, 'HEADPHONES-001', true, NOW(), NOW()),
(12, 'Smart Watch', 'Fitness tracking smartwatch', 299.99, 55, 1, 'SMARTWATCH-001', true, NOW(), NOW());

-- Insert test product images
INSERT INTO product_images (id, product_id, image_url, is_main, created_at, updated_at) VALUES
(1, 1, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400', true, NOW(), NOW()),
(2, 1, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400&v=2', false, NOW(), NOW()),
(3, 2, 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=400', true, NOW(), NOW()),
(4, 3, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400', true, NOW(), NOW()),
(5, 4, 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=400', true, NOW(), NOW()),
(6, 5, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400', true, NOW(), NOW()),
(7, 6, 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400', true, NOW(), NOW()),
(8, 7, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400', true, NOW(), NOW()),
(9, 8, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400', true, NOW(), NOW()),
(10, 9, 'https://images.unsplash.com/photo-1592078615290-033ee584e267?w=400', true, NOW(), NOW()),
(11, 10, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=400', true, NOW(), NOW()),
(12, 11, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400', true, NOW(), NOW()),
(13, 12, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400', true, NOW(), NOW());

-- Insert test product specifications
INSERT INTO product_specifications (id, product_id, name, spec_value, unit, display_order, created_at, updated_at) VALUES
(1, 1, 'Screen Size', '6.1', 'inches', 1, NOW(), NOW()),
(2, 1, 'Storage', '128', 'GB', 2, NOW(), NOW()),
(3, 1, 'Color', 'Titanium', NULL, 3, NOW(), NOW()),
(4, 2, 'Screen Size', '6.2', 'inches', 1, NOW(), NOW()),
(5, 2, 'Storage', '256', 'GB', 2, NOW(), NOW()),
(6, 2, 'Color', 'Phantom Black', NULL, 3, NOW(), NOW()),
(7, 3, 'Processor', 'M3 Pro', NULL, 1, NOW(), NOW()),
(8, 3, 'Memory', '16', 'GB', 2, NOW(), NOW()),
(9, 3, 'Storage', '512', 'GB', 3, NOW(), NOW()),
(10, 5, 'Material', 'Cotton', NULL, 1, NOW(), NOW()),
(11, 5, 'Size', 'M', NULL, 2, NOW(), NOW()),
(12, 6, 'Material', 'Polyester', NULL, 1, NOW(), NOW()),
(13, 6, 'Size', 'S', NULL, 2, NOW(), NOW());



-- Insert test users (admin and regular users)
INSERT INTO users (id, email, first_name, last_name, cognito_id, role, enabled, created_at, updated_at) VALUES
(1, 'admin@ecommerce.com', 'Admin', 'User', 'admin-cognito-id', 'ADMIN', true, NOW(), NOW()),
(2, 'john.doe@example.com', 'John', 'Doe', 'john-cognito-id', 'USER', true, NOW(), NOW()),
(3, 'jane.smith@example.com', 'Jane', 'Smith', 'jane-cognito-id', 'USER', true, NOW(), NOW()),
(4, 'mike.wilson@example.com', 'Mike', 'Wilson', 'mike-cognito-id', 'USER', true, NOW(), NOW()),
(5, 'sarah.johnson@example.com', 'Sarah', 'Johnson', 'sarah-cognito-id', 'USER', true, NOW(), NOW()),
(6, 'david.brown@example.com', 'David', 'Brown', 'david-cognito-id', 'USER', true, NOW(), NOW());

-- Insert test addresses for users
INSERT INTO addresses (id, user_id, street, city, state, country, postal_code, created_at, updated_at) VALUES
(1, 2, '123 Main St', 'New York', 'NY', 'USA', '10001', NOW(), NOW()),
(2, 3, '456 Oak Ave', 'Los Angeles', 'CA', 'USA', '90210', NOW(), NOW()),
(3, 4, '789 Pine Rd', 'Chicago', 'IL', 'USA', '60601', NOW(), NOW()),
(4, 5, '321 Elm St', 'Houston', 'TX', 'USA', '77001', NOW(), NOW()),
(5, 6, '654 Maple Dr', 'Phoenix', 'AZ', 'USA', '85001', NOW(), NOW());

-- Insert test product reviews (after users are created)
INSERT INTO product_reviews (id, product_id, user_id, rating, comment, created_at, updated_at) VALUES
(1, 1, 1, 5, 'Excellent phone! The camera quality is amazing.', NOW(), NOW()),
(2, 1, 2, 4, 'Great performance, but battery could be better.', NOW(), NOW()),
(3, 2, 3, 5, 'Best Android phone I have ever used.', NOW(), NOW()),
(4, 3, 1, 5, 'Perfect for development work. Very fast!', NOW(), NOW()),
(5, 5, 4, 4, 'Comfortable and good quality fabric.', NOW(), NOW()),
(6, 6, 5, 5, 'Beautiful dress, perfect fit!', NOW(), NOW()),
(7, 7, 6, 5, 'Classic literature at its best.', NOW(), NOW()),
(8, 8, 1, 5, 'Must-read for every developer.', NOW(), NOW());

-- Insert test orders
INSERT INTO orders (id, order_number, user_id, status, payment_status, subtotal, shipping_cost, tax, total, payment_id, payer_id, payment_method, payment_date, created_at, updated_at) VALUES
(1, 'ORD-2024-001', 2, 'CONFIRMED', 'PAID', 1029.98, 15.00, 82.40, 1127.38, 'PAY-001', 'PAYER-001', 'PAYPAL', NOW(), NOW(), NOW()),
(2, 'ORD-2024-002', 3, 'SHIPPED', 'PAID', 79.99, 10.00, 6.40, 96.39, 'PAY-002', 'PAYER-002', 'PAYPAL', NOW(), NOW(), NOW()),
(3, 'ORD-2024-003', 4, 'DELIVERED', 'PAID', 199.99, 0.00, 16.00, 215.99, 'PAY-003', 'PAYER-003', 'PAYPAL', NOW(), NOW(), NOW()),
(4, 'ORD-2024-004', 5, 'PENDING', 'PENDING', 149.98, 12.00, 12.00, 173.98, NULL, NULL, NULL, NULL, NOW(), NOW());

-- Insert test order items
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price, created_at, updated_at) VALUES
(1, 1, 1, 1, 999.99, 999.99, NOW(), NOW()),
(2, 1, 5, 1, 29.99, 29.99, NOW(), NOW()),
(3, 2, 6, 1, 79.99, 79.99, NOW(), NOW()),
(4, 3, 11, 1, 199.99, 199.99, NOW(), NOW()),
(5, 4, 10, 2, 49.99, 99.98, NOW(), NOW()),
(6, 4, 7, 1, 12.99, 12.99, NOW(), NOW()),
(7, 4, 8, 1, 39.99, 39.99, NOW(), NOW());

-- Insert test shipping info
INSERT INTO shipping_info (id, order_id, tracking_number, status, carrier, estimated_delivery, actual_delivery, created_at, updated_at) VALUES
(1, 1, 'TRK-001-123456', 'IN_TRANSIT', 'FedEx', NOW() + INTERVAL '3 days', NULL, NOW(), NOW()),
(2, 2, 'TRK-002-789012', 'DELIVERED', 'UPS', NOW() + INTERVAL '2 days', NOW() - INTERVAL '1 day', NOW(), NOW()),
(3, 3, 'TRK-003-345678', 'DELIVERED', 'USPS', NOW() + INTERVAL '5 days', NOW() - INTERVAL '2 days', NOW(), NOW());

-- Insert test product reservations
INSERT INTO product_reservations (id, product_id, user_id, quantity, reserved_at, expires_at, active, created_at, updated_at) VALUES
(1, 1, 2, 1, NOW(), NOW() + INTERVAL '30 minutes', true, NOW(), NOW()),
(2, 3, 3, 1, NOW(), NOW() + INTERVAL '30 minutes', true, NOW(), NOW()),
(3, 11, 4, 2, NOW(), NOW() + INTERVAL '30 minutes', true, NOW(), NOW());

-- Update sequences to start after the inserted data
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
SELECT setval('product_images_id_seq', (SELECT MAX(id) FROM product_images));
SELECT setval('product_reviews_id_seq', (SELECT MAX(id) FROM product_reviews));
SELECT setval('product_specifications_id_seq', (SELECT MAX(id) FROM product_specifications));
SELECT setval('product_reservations_id_seq', (SELECT MAX(id) FROM product_reservations));
SELECT setval('shipping_info_id_seq', (SELECT MAX(id) FROM shipping_info));

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id ON product_reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_product_specifications_product_id ON product_specifications(product_id);
CREATE INDEX IF NOT EXISTS idx_product_reservations_product_id ON product_reservations(product_id);
CREATE INDEX IF NOT EXISTS idx_product_reservations_user_id ON product_reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_shipping_info_order_id ON shipping_info(order_id);
CREATE INDEX IF NOT EXISTS idx_shipping_info_tracking_number ON shipping_info(tracking_number);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ecommerce_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ecommerce_user; 