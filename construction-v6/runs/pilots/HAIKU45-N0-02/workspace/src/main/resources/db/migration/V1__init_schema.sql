CREATE TABLE produto (
    sku VARCHAR(50) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    peso_kg NUMERIC(10, 5) NOT NULL,
    estoque INT NOT NULL,
    fora_de_linha BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE cliente (
    cpf VARCHAR(11) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    saldo_credito NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    nivel_clube VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pedido (
    id BIGSERIAL PRIMARY KEY,
    cpf_cliente VARCHAR(11) NOT NULL,
    situacao VARCHAR(50) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resumo JSONB NOT NULL,
    codigo_rastreio VARCHAR(100),
    id_transacao VARCHAR(100),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cpf_cliente) REFERENCES cliente(cpf)
);

CREATE TABLE pedido_item (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    quantidade INT NOT NULL,
    CONSTRAINT fk_pedido_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id),
    CONSTRAINT fk_pedido_item_produto FOREIGN KEY (sku) REFERENCES produto(sku)
);

CREATE TABLE aviso (
    id BIGSERIAL PRIMARY KEY,
    cpf_cliente VARCHAR(11) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    pedido_id BIGINT NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'NOVO',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aviso_cliente FOREIGN KEY (cpf_cliente) REFERENCES cliente(cpf),
    CONSTRAINT fk_aviso_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id)
);

CREATE TABLE pagamento (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    id_transacao VARCHAR(100) NOT NULL UNIQUE,
    pago_em TIMESTAMP NOT NULL,
    registrado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagamento_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id)
);

CREATE INDEX idx_pedido_cpf ON pedido(cpf_cliente);
CREATE INDEX idx_pedido_situacao ON pedido(situacao);
CREATE INDEX idx_aviso_cpf ON aviso(cpf_cliente);
CREATE INDEX idx_aviso_pedido ON aviso(pedido_id);
CREATE INDEX idx_pedido_item_pedido ON pedido_item(pedido_id);
