CREATE TABLE products (
  sku VARCHAR(20) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  price NUMERIC(10, 2) NOT NULL,
  weight_kg NUMERIC(10, 6) NOT NULL,
  stock INTEGER NOT NULL,
  discontinued BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE customers (
  cpf VARCHAR(11) PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  credit_balance NUMERIC(10, 2) NOT NULL DEFAULT 0
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  customer_cpf VARCHAR(11) NOT NULL REFERENCES customers(cpf),
  customer_email VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  tracking_code VARCHAR(255),
  region VARCHAR(30) NOT NULL,
  shipping_method VARCHAR(30) NOT NULL,
  zip_code VARCHAR(8) NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  installments INTEGER NOT NULL,
  club_tier VARCHAR(30) NOT NULL,
  coupon VARCHAR(50),
  transaction_id VARCHAR(255),
  paid_at TIMESTAMP,
  products_subtotal NUMERIC(10, 2),
  coupon_discount NUMERIC(10, 2),
  shipping_cost NUMERIC(10, 2),
  delivery_days INTEGER,
  insurance NUMERIC(10, 2),
  payment_adjustment NUMERIC(10, 2),
  final_total NUMERIC(10, 2),
  installments_summary INTEGER,
  installment_amount NUMERIC(10, 2),
  next_purchase_credit NUMERIC(10, 2),
  gift BOOLEAN
);

CREATE TABLE order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES orders(id),
  sku VARCHAR(20) NOT NULL REFERENCES products(sku),
  quantity INTEGER NOT NULL,
  price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  customer_cpf VARCHAR(11) NOT NULL,
  type VARCHAR(30) NOT NULL,
  order_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_customer ON orders(customer_cpf);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_notifications_customer ON notifications(customer_cpf);
CREATE INDEX idx_notifications_order_type ON notifications(order_id, type);
