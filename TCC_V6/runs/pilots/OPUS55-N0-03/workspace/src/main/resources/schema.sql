CREATE TABLE IF NOT EXISTS product (
    sku          VARCHAR(64)   PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    price        NUMERIC(12,2) NOT NULL,
    weight_kg    NUMERIC(10,3) NOT NULL,
    stock        INTEGER       NOT NULL CHECK (stock >= 0),
    discontinued BOOLEAN       NOT NULL
);

CREATE TABLE IF NOT EXISTS customer (
    cpf            VARCHAR(11)   PRIMARY KEY,
    credit_balance NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_order (
    id                     VARCHAR(36)   PRIMARY KEY,
    cpf                    VARCHAR(11)   NOT NULL REFERENCES customer (cpf),
    email                  VARCHAR(320)  NOT NULL,
    status                 VARCHAR(32)   NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    tracking_code          VARCHAR(255),
    payment_transaction_id VARCHAR(255),
    paid_at                TIMESTAMP WITH TIME ZONE,
    products_subtotal      NUMERIC(12,2) NOT NULL,
    coupon_discount        NUMERIC(12,2) NOT NULL,
    shipping_cost          NUMERIC(12,2) NOT NULL,
    delivery_days          INTEGER       NOT NULL,
    insurance              NUMERIC(12,2) NOT NULL,
    payment_adjustment     NUMERIC(12,2) NOT NULL,
    final_total            NUMERIC(12,2) NOT NULL,
    installments           INTEGER       NOT NULL,
    installment_amount     NUMERIC(12,2) NOT NULL,
    next_purchase_credit   NUMERIC(12,2) NOT NULL,
    gift                   BOOLEAN       NOT NULL
);

CREATE INDEX IF NOT EXISTS customer_order_cpf_status ON customer_order (cpf, status);

CREATE TABLE IF NOT EXISTS order_item (
    order_id VARCHAR(36) NOT NULL REFERENCES customer_order (id),
    line_no  INTEGER     NOT NULL,
    sku      VARCHAR(64) NOT NULL REFERENCES product (sku),
    quantity INTEGER     NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE TABLE IF NOT EXISTS payment_notice (
    order_id       VARCHAR(36)  NOT NULL REFERENCES customer_order (id),
    transaction_id VARCHAR(255) NOT NULL,
    outcome        VARCHAR(32)  NOT NULL,
    PRIMARY KEY (order_id, transaction_id)
);

CREATE TABLE IF NOT EXISTS notification (
    id       VARCHAR(36) PRIMARY KEY,
    seq      BIGINT      GENERATED ALWAYS AS IDENTITY,
    cpf      VARCHAR(11) NOT NULL REFERENCES customer (cpf),
    order_id VARCHAR(36) NOT NULL REFERENCES customer_order (id),
    type     VARCHAR(32) NOT NULL,
    status   VARCHAR(16) NOT NULL
);

CREATE INDEX IF NOT EXISTS notification_cpf ON notification (cpf, seq);
