CREATE TABLE products (
  sku VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  price NUMERIC(12,2) NOT NULL,
  weight_kg NUMERIC(10,3) NOT NULL,
  stock INTEGER NOT NULL CHECK (stock >= 0),
  discontinued BOOLEAN NOT NULL
);

INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES
  ('CAM-001', 'Camiseta básica', 79.90, 0.30, 50, FALSE),
  ('TEN-001', 'Tênis corrida', 249.90, 1.20, 10, FALSE),
  ('FON-001', 'Fone sem fio', 199.90, 0.25, 5, FALSE),
  ('MEI-001', 'Meia esportiva', 19.90, 0.10, 100, FALSE),
  ('JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, FALSE),
  ('MOC-001', 'Mochila urbana', 159.90, 0.90, 1, FALSE),
  ('BON-001', 'Boné aba reta', 59.90, 0.15, 0, FALSE),
  ('REL-001', 'Relógio digital', 129.90, 0.20, 8, TRUE);

CREATE TABLE customers (
  cpf VARCHAR(255) PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  credit_balance NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY,
  cpf VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  tracking_code VARCHAR(255),
  products_subtotal NUMERIC(14,2) NOT NULL,
  coupon_discount NUMERIC(14,2) NOT NULL,
  shipping_cost NUMERIC(14,2) NOT NULL,
  delivery_days INTEGER NOT NULL,
  insurance NUMERIC(14,2) NOT NULL,
  payment_adjustment NUMERIC(14,2) NOT NULL,
  final_total NUMERIC(14,2) NOT NULL,
  installments INTEGER NOT NULL,
  installment_amount NUMERIC(14,2) NOT NULL,
  next_purchase_credit NUMERIC(14,2) NOT NULL,
  gift BOOLEAN NOT NULL
);
CREATE INDEX idx_orders_cpf_status ON orders (cpf, status);

CREATE TABLE order_items (
  order_id VARCHAR(36) NOT NULL,
  position INTEGER NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INTEGER NOT NULL,
  PRIMARY KEY (order_id, position)
);

CREATE TABLE payment_events (
  order_id VARCHAR(36) NOT NULL,
  transaction_id VARCHAR(255) NOT NULL,
  outcome VARCHAR(64) NOT NULL,
  PRIMARY KEY (order_id, transaction_id)
);

CREATE TABLE notifications (
  seq BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id VARCHAR(36) NOT NULL UNIQUE,
  cpf VARCHAR(255) NOT NULL,
  type VARCHAR(32) NOT NULL,
  order_id VARCHAR(36) NOT NULL,
  status VARCHAR(8) NOT NULL
);
CREATE INDEX idx_notifications_cpf ON notifications (cpf, seq);
