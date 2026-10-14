CREATE TABLE IF NOT EXISTS produto (
  sku            VARCHAR(60) PRIMARY KEY,
  nome           VARCHAR(200) NOT NULL,
  preco          NUMERIC(12,2) NOT NULL,
  peso_kg        NUMERIC(10,3) NOT NULL,
  estoque        INTEGER NOT NULL CHECK (estoque >= 0),
  fora_de_linha  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS cliente (
  cpf            VARCHAR(11) PRIMARY KEY,
  email          VARCHAR(200) NOT NULL,
  saldo_credito  NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pedido (
  id                       VARCHAR(36) PRIMARY KEY,
  cpf                      VARCHAR(11) NOT NULL REFERENCES cliente(cpf),
  email                    VARCHAR(200) NOT NULL,
  cep                      VARCHAR(8) NOT NULL,
  regiao                   VARCHAR(20) NOT NULL,
  modalidade_entrega       VARCHAR(40) NOT NULL,
  cupom                    VARCHAR(40),
  forma_pagamento          VARCHAR(20) NOT NULL,
  nivel_clube              VARCHAR(40) NOT NULL,
  situacao                 VARCHAR(30) NOT NULL,
  criado_em                TIMESTAMP WITH TIME ZONE NOT NULL,
  codigo_rastreio          VARCHAR(100),
  id_transacao             VARCHAR(100),
  subtotal_produtos        NUMERIC(12,2) NOT NULL,
  desconto_cupom           NUMERIC(12,2) NOT NULL,
  frete                    NUMERIC(12,2) NOT NULL,
  prazo_entrega_dias       INTEGER NOT NULL,
  seguro                   NUMERIC(12,2) NOT NULL,
  ajuste_pagamento         NUMERIC(12,2) NOT NULL,
  total_final              NUMERIC(12,2) NOT NULL,
  parcelas                 INTEGER NOT NULL,
  valor_parcela            NUMERIC(12,2) NOT NULL,
  credito_proxima_compra   NUMERIC(12,2) NOT NULL,
  brinde                   BOOLEAN NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pedido_cpf_situacao ON pedido (cpf, situacao);

CREATE TABLE IF NOT EXISTS pedido_item (
  pedido_id   VARCHAR(36) NOT NULL REFERENCES pedido(id),
  posicao     INTEGER NOT NULL,
  sku         VARCHAR(60) NOT NULL,
  quantidade  INTEGER NOT NULL,
  PRIMARY KEY (pedido_id, posicao)
);

CREATE TABLE IF NOT EXISTS aviso (
  seq        BIGINT GENERATED ALWAYS AS IDENTITY,
  id         VARCHAR(36) PRIMARY KEY,
  cpf        VARCHAR(11) NOT NULL REFERENCES cliente(cpf),
  tipo       VARCHAR(40) NOT NULL,
  pedido_id  VARCHAR(36) NOT NULL,
  situacao   VARCHAR(10) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_aviso_cpf ON aviso (cpf, seq);
