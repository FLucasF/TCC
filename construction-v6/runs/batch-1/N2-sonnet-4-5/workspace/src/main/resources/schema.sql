CREATE TABLE IF NOT EXISTS products (
    sku VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    weight_kg NUMERIC(10,3) NOT NULL,
    stock INTEGER NOT NULL CHECK (stock >= 0),
    discontinued BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS customers (
    cpf VARCHAR(11) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    credit_balance NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (credit_balance >= 0),
    club_tier VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY,
    customer_cpf VARCHAR(11) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    zip_code VARCHAR(8) NOT NULL,
    region VARCHAR(20) NOT NULL,
    shipping_method VARCHAR(20) NOT NULL,
    coupon VARCHAR(20),
    payment_method VARCHAR(20) NOT NULL,
    installments INTEGER NOT NULL,
    club_tier VARCHAR(20) NOT NULL,

    products_subtotal NUMERIC(10,2) NOT NULL,
    coupon_discount NUMERIC(10,2) NOT NULL,
    shipping_cost NUMERIC(10,2) NOT NULL,
    delivery_days INTEGER NOT NULL,
    insurance NUMERIC(10,2) NOT NULL,
    payment_adjustment NUMERIC(10,2) NOT NULL,
    final_total NUMERIC(10,2) NOT NULL,
    installment_amount NUMERIC(10,2) NOT NULL,
    next_purchase_credit NUMERIC(10,2) NOT NULL,
    gift BOOLEAN NOT NULL,

    tracking_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (customer_cpf) REFERENCES customers(cpf)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (sku) REFERENCES products(sku)
);

CREATE TABLE IF NOT EXISTS order_payments (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    transaction_id VARCHAR(100) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_cpf VARCHAR(11) NOT NULL,
    type VARCHAR(30) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_cpf) REFERENCES customers(cpf),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_cpf);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_customer ON notifications(customer_cpf);

INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES
('CAM-001', 'Camiseta básica', 79.90, 0.30, 50, FALSE),
('TEN-001', 'Tênis corrida', 249.90, 1.20, 10, FALSE),
('FON-001', 'Fone sem fio', 199.90, 0.25, 5, FALSE),
('MEI-001', 'Meia esportiva', 19.90, 0.10, 100, FALSE),
('JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, FALSE),
('MOC-001', 'Mochila urbana', 159.90, 0.90, 1, FALSE),
('BON-001', 'Boné aba reta', 59.90, 0.15, 0, FALSE),
('REL-001', 'Relógio digital', 129.90, 0.20, 8, TRUE)
ON CONFLICT (sku) DO NOTHING;
