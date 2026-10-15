-- Catálogo inicial. Só entra o que ainda não existe: reiniciar o serviço não mexe no estoque.
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'CAM-001', 'Camiseta básica', 79.90, 0.30, 50, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'CAM-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'TEN-001', 'Tênis corrida', 249.90, 1.20, 10, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'TEN-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'FON-001', 'Fone sem fio', 199.90, 0.25, 5, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'FON-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'MEI-001', 'Meia esportiva', 19.90, 0.10, 100, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'MEI-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'JAQ-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'MOC-001', 'Mochila urbana', 159.90, 0.90, 1, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'MOC-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'BON-001', 'Boné aba reta', 59.90, 0.15, 0, FALSE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'BON-001');
INSERT INTO product (sku, name, price, weight_kg, stock, discontinued)
SELECT 'REL-001', 'Relógio digital', 129.90, 0.20, 8, TRUE WHERE NOT EXISTS (SELECT 1 FROM product WHERE sku = 'REL-001');
