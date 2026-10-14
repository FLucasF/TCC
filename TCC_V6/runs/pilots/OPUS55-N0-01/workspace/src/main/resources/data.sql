-- Catálogo inicial: só entra o que ainda não existe, para não desfazer o estoque já movimentado.
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'CAM-001', 'Camiseta básica', 79.90, 0.30, 50, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'CAM-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'TEN-001', 'Tênis corrida', 249.90, 1.20, 10, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'TEN-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'FON-001', 'Fone sem fio', 199.90, 0.25, 5, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'FON-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'MEI-001', 'Meia esportiva', 19.90, 0.10, 100, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'MEI-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'JAQ-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'MOC-001', 'Mochila urbana', 159.90, 0.90, 1, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'MOC-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'BON-001', 'Boné aba reta', 59.90, 0.15, 0, FALSE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'BON-001');
INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
SELECT 'REL-001', 'Relógio digital', 129.90, 0.20, 8, TRUE WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'REL-001');
