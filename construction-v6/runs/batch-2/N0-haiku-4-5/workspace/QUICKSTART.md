# Quick Start

## 5 Minutos para ter o Serviço Rodando

### 1. Preparar o PostgreSQL

```bash
# Instalar (se não tiver)
sudo apt-get install postgresql postgresql-contrib

# Criar banco
sudo -u postgres createdb orderservice

# Conectar e conferir
psql -U postgres -d orderservice -c "SELECT version();"
```

### 2. Compilar o Projeto

```bash
cd /workspace
mvn clean package -DskipTests
```

Resultado: `target/order-service-1.0.0.jar` (7.4 MB)

### 3. Executar o Serviço

```bash
java -jar target/order-service-1.0.0.jar
```

Você verá:
```
[main] INFO org.eclipse.jetty.server.Server - Started Server@...
Order service started on port 8080
```

**Pronto!** O serviço está rodando em `http://localhost:8080`

## Teste Rápido

### 1. Criar um Pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678901",
      "email": "cliente@example.com"
    },
    "items": [
      {"sku": "CAM-001", "quantity": 2}
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }' | jq .
```

Resultado:
```json
{
  "id": "uuid-do-pedido",
  "status": "AWAITING_PAYMENT",
  "summary": {
    "productsSubtotal": 159.80,
    "finalTotal": 152.73,
    ...
  },
  ...
}
```

### 2. Confirmar Pagamento

```bash
PEDIDO_ID="uuid-do-pedido"

curl -X POST http://localhost:8080/orders/$PEDIDO_ID/payment \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX-001",
    "paidAt": "2026-10-18T14:00:00Z"
  }' | jq .
```

### 3. Consultar Pedido

```bash
curl http://localhost:8080/orders/$PEDIDO_ID | jq .
```

Agora o status será `PAID`

## Comandos Úteis

### Listar Produtos

```bash
curl http://localhost:8080/products/CAM-001 | jq .
```

### Consultar Saldo de Crédito

```bash
curl http://localhost:8080/customers/12345678901 | jq .
```

### Ver Notificações

```bash
curl http://localhost:8080/customers/12345678901/notifications | jq .
```

### Marcar Notificação como Lida

```bash
curl -X POST http://localhost:8080/notifications/uuid-notificacao/read | jq .
```

## Configuração Customizada

### Com Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://seu-servidor:5432/seu-banco"
export DB_USER="seu-usuario"
export DB_PASSWORD="sua-senha"
export PORT="3000"
export TRANSLOG_URL="https://sua-translog.api.com"

java -jar target/order-service-1.0.0.jar
```

### Em Arquivo Shell

```bash
cat > run.sh << 'EOF'
#!/bin/bash
export DB_URL="jdbc:postgresql://localhost:5432/orderservice"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="8080"
export TRANSLOG_URL="https://api.translog.com.br"

java -jar target/order-service-1.0.0.jar
EOF

chmod +x run.sh
./run.sh
```

## Testar com Postman/Insomnia

1. Importar coleção (criar uma):
   - Base URL: `http://localhost:8080`
   - POST /orders
   - POST /orders/{id}/payment
   - GET /orders/{id}
   - etc

2. Ou usar o arquivo EXAMPLES.md para exemplos prontos

## Estrutura de Diretórios

```
/workspace/
├── pom.xml                          # Configuração Maven
├── README.md                        # Documentação principal
├── QUICKSTART.md                    # Este arquivo
├── EXAMPLES.md                      # Exemplos de API
├── ARQUITETURA.md                   # Design do sistema
├── DEPLOYMENT.md                    # Guia de produção
├── src/
│   ├── main/java/com/loja/         # Código-fonte
│   │   ├── App.java                # Classe principal
│   │   ├── api/                    # Rotas e DTOs
│   │   ├── database/               # Banco de dados
│   │   ├── models/                 # Modelos
│   │   ├── services/               # Lógica de negócio
│   │   └── utils/                  # Utilidades
│   └── test/java/                  # Testes
└── target/
    └── order-service-1.0.0.jar     # JAR executável
```

## Produtos Disponíveis

No catálogo inicial:

| SKU | Nome | Preço | Peso | Estoque |
|-----|------|-------|------|---------|
| CAM-001 | Camiseta básica | 79,90 | 0,30kg | 50 |
| TEN-001 | Tênis corrida | 249,90 | 1,20kg | 10 |
| FON-001 | Fone sem fio | 199,90 | 0,25kg | 5 |
| MEI-001 | Meia esportiva | 19,90 | 0,10kg | 100 |
| JAQ-001 | Jaqueta corta-vento | 389,90 | 0,80kg | 3 |
| MOC-001 | Mochila urbana | 159,90 | 0,90kg | 1 |
| BON-001 | Boné aba reta | 59,90 | 0,15kg | 0 |
| REL-001 | Relógio digital | 129,90 | 0,20kg | 8 (descontinuado) |

## Regiões Suportadas

- `SOUTHEAST` (Sudeste)
- `SOUTH` (Sul)
- `MIDWEST` (Centro-Oeste)
- `NORTH` (Norte)
- `NORTHEAST` (Nordeste)

## Formas de Entrega

- `STORE_PICKUP` - Retirada na loja (grátis, 1 dia)
- `COURIER` - Motoboy (R$ 18,00, mesmo dia, até 5kg)
- `ECONOMY` - Econômica (consulta TransLog)
- `EXPRESS` - Expressa (consulta TransLog)

## Formas de Pagamento

- `PIX` - 5% desconto (se sem cupom)
- `CREDIT_CARD` - Até 12x (com juros acima de 3x)
- `BANK_SLIP` - Boleto (+R$ 3,49, máx R$ 1.000,00)

## Cupons Disponíveis

- `WELCOME10` - 10% (primeira compra)
- `MINUS50` - R$ 50 off (mín R$ 300)
- `FREESHIPPING` - Frete grátis
- `BUY3PAY2` - Leve 3 pague 2

## Níveis de Clube

- `BRONZE` - Sem benefícios
- `SILVER` - 2% crédito
- `GOLD` - 5% crédito + frete grátis + brinde (>R$ 500)

## Debugging

### Ver logs detalhados

```bash
# Terminal 1 - Executar com debug
java -jar target/order-service-1.0.0.jar

# Terminal 2 - Fazer requisição
curl -X POST http://localhost:8080/orders ...

# Logs aparecem no Terminal 1
```

### Verificar banco de dados

```bash
psql -U postgres -d orderservice -c "SELECT * FROM products;"
psql -U postgres -d orderservice -c "SELECT * FROM orders;"
psql -U postgres -d orderservice -c "SELECT * FROM customers;"
```

## Troubleshooting

### Porta já em uso

```bash
# Usar outra porta
PORT=3000 java -jar target/order-service-1.0.0.jar
```

### Banco de dados não conecta

```bash
# Verificar se PostgreSQL está rodando
psql -U postgres -c "SELECT 1;"

# Verificar credentials
DB_URL="jdbc:postgresql://localhost:5432/orderservice" \
DB_USER="postgres" \
DB_PASSWORD="postgres" \
java -jar target/order-service-1.0.0.jar
```

### Erro ao compilar

```bash
# Limpar cache
mvn clean

# Recompilar com verbose
mvn -e compile
```

## Próximos Passos

1. Ler [README.md](README.md) para visão geral
2. Ver [EXAMPLES.md](EXAMPLES.md) para exemplos de API
3. Estudar [ARQUITETURA.md](ARQUITETURA.md) para entender o design
4. Consultar [DEPLOYMENT.md](DEPLOYMENT.md) para colocar em produção

## Suporte

Se tiver dúvidas:
1. Verifique os exemplos em EXAMPLES.md
2. Consulte a documentação em ARQUITETURA.md
3. Veja as respostas esperadas em cada endpoint
4. Check logs do serviço para erros

---

**Sucesso!** 🚀 Você agora tem um serviço de pedidos completo rodando.
