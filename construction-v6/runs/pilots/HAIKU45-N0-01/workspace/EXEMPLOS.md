# Exemplos de Uso

## 1. Calcular Resumo da Compra

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": {
      "cpf": "12345678901",
      "email": "cliente@exemplo.com"
    },
    "itens": [
      {
        "sku": "CAM-001",
        "quantidade": 2
      },
      {
        "sku": "MEI-001",
        "quantidade": 1
      }
    ],
    "cep": "01310100",
    "regiao": "SUDESTE",
    "modalidadeEntrega": "RETIRADA_LOJA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "PIX",
    "nivelClube": "BRONZE"
  }'
```

Resposta:
```json
{
  "subtotalProdutos": 179.70,
  "descontoCupom": 17.97,
  "frete": 0.00,
  "prazoEntregaDias": 1,
  "seguro": 1.72,
  "ajustePagamento": -8.09,
  "totalFinal": 155.36,
  "parcelas": 1,
  "valorParcela": 155.36,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

## 2. Registrar Pedido

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": {
      "cpf": "12345678901",
      "email": "cliente@exemplo.com"
    },
    "itens": [
      {
        "sku": "CAM-001",
        "quantidade": 1
      }
    ],
    "cep": "01310100",
    "regiao": "SUDESTE",
    "modalidadeEntrega": "RETIRADA_LOJA",
    "formaPagamento": "PIX",
    "nivelClube": "BRONZE"
  }'
```

Resposta (HTTP 201):
```json
{
  "id": 1,
  "situacao": "AGUARDANDO_PAGAMENTO",
  "criadoEm": "2026-10-14T15:30:00Z",
  "resumo": {
    "subtotalProdutos": 79.90,
    "descontoCupom": 0.00,
    "frete": 0.00,
    "prazoEntregaDias": 1,
    "seguro": 0.80,
    "ajustePagamento": -4.04,
    "totalFinal": 76.66,
    "parcelas": 1,
    "valorParcela": 76.66,
    "creditoProximaCompra": 0.00,
    "brinde": false
  },
  "itens": [
    {
      "sku": "CAM-001",
      "quantidade": 1
    }
  ],
  "codigoRastreio": ""
}
```

## 3. Registrar Pagamento

```bash
curl -X POST http://localhost:8080/pedidos/1/pagamento \
  -H "Content-Type: application/json" \
  -d '{
    "idTransacao": "TX-123456",
    "pagoEm": "2026-10-14T15:35:00Z"
  }'
```

Resposta (HTTP 200):
```json
{
  "id": 1,
  "situacao": "PAGO",
  "criadoEm": "2026-10-14T15:30:00Z",
  "resumo": { ... },
  "itens": [ ... ],
  "codigoRastreio": ""
}
```

## 4. Registrar Envio

```bash
curl -X POST http://localhost:8080/pedidos/1/envio \
  -H "Content-Type: application/json" \
  -d '{
    "codigoRastreio": "BR123456789ABC"
  }'
```

## 5. Registrar Entrega

```bash
curl -X POST http://localhost:8080/pedidos/1/entrega
```

## 6. Cancelar Pedido

```bash
curl -X POST http://localhost:8080/pedidos/1/cancelamento
```

## 7. Consultar Pedido

```bash
curl http://localhost:8080/pedidos/1
```

## 8. Consultar Produto

```bash
curl http://localhost:8080/produtos/CAM-001
```

Resposta:
```json
{
  "sku": "CAM-001",
  "nome": "Camiseta básica",
  "preco": 79.90,
  "pesoKg": 0.30,
  "estoque": 49,
  "foraDeLinha": false
}
```

## 9. Consultar Saldo do Cliente

```bash
curl http://localhost:8080/clientes/12345678901
```

Resposta:
```json
{
  "cpf": "12345678901",
  "saldoCredito": 5.99
}
```

## 10. Consultar Avisos do Cliente

```bash
curl http://localhost:8080/clientes/12345678901/avisos
```

Resposta:
```json
[
  {
    "id": 1,
    "tipo": "PAGAMENTO_APROVADO",
    "pedidoId": 1,
    "situacao": "NOVO"
  }
]
```

## 11. Marcar Aviso como Lido

```bash
curl -X POST http://localhost:8080/avisos/1/leitura
```

## Exemplos de Erro

### Carrinho Vazio
```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": {
      "cpf": "12345678901",
      "email": "cliente@exemplo.com"
    },
    "itens": [],
    "cep": "01310100",
    "regiao": "SUDESTE",
    "modalidadeEntrega": "RETIRADA_LOJA",
    "formaPagamento": "PIX",
    "nivelClube": "BRONZE"
  }'
```

Resposta (HTTP 422):
```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

### Produto Inexistente
Resposta (HTTP 422):
```json
{
  "erro": "PRODUTO_INEXISTENTE"
}
```

### Estoque Insuficiente
Resposta (HTTP 422):
```json
{
  "erro": "ESTOQUE_INSUFICIENTE"
}
```
