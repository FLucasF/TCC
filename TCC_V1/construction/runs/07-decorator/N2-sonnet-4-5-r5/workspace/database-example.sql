-- Exemplo de criação manual do banco de dados
-- (O sistema cria automaticamente na inicialização)

CREATE DATABASE pousada;

-- Conectar ao banco
\c pousada

-- As tabelas são criadas automaticamente pelo sistema na primeira execução
-- Este arquivo é apenas para referência

-- Para limpar o banco:
-- DROP TABLE IF EXISTS messages CASCADE;
-- DROP TABLE IF EXISTS reservations CASCADE;
-- DROP TABLE IF EXISTS rooms CASCADE;
