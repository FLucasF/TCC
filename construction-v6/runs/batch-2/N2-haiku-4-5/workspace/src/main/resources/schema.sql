CREATE TABLE IF NOT EXISTS products (
    sku VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    weight_kg DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    discontinued BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS customers (
    cpf VARCHAR(11) PRIMARY KEY,
    credit_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL REFERENCES customers(cpf),
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    tracking_code VARCHAR(50),
    summary_json TEXT NOT NULL,
    items_json TEXT NOT NULL,
    UNIQUE(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    order_id VARCHAR(36),
    cpf VARCHAR(11) NOT NULL,
    status VARCHAR(50) NOT NULL,
    REFERENCES orders(id),
    UNIQUE(id)
);

CREATE TABLE IF NOT EXISTS payment_tracking (
    order_id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL,
    REFERENCES orders(id),
    UNIQUE(order_id, transaction_id)
);

INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES
('CAM-001', 'Camiseta básica', 79.90, 0.30, 50, FALSE),
('TEN-001', 'Tênis corrida', 249.90, 1.20, 10, FALSE),
('FON-001', 'Fone sem fio', 199.90, 0.25, 5, FALSE),
('MEI-001', 'Meia esportiva', 19.90, 0.10, 100, FALSE),
('JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, FALSE),
('MOC-001', 'Mochila urbana', 159.90, 0.90, 1, FALSE),
('BON-001', 'Boné aba reta', 59.90, 0.15, 0, FALSE),
('REL-001', 'Relógio digital', 129.90, 0.20, 8, TRUE)
ON CONFLICT DO NOTHING;
