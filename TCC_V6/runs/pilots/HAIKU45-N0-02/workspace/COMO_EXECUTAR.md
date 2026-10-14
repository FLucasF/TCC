# Como Executar o Serviço Localmente

## Pré-requisitos

1. **Java 21**: https://adoptopenjdk.net/
2. **Maven 3.8+**: https://maven.apache.org/download.cgi
3. **PostgreSQL 18**: https://www.postgresql.org/download/

## Passo 1: Preparar o Banco de Dados

### Opção A: PostgreSQL Local

```bash
# Criar banco de dados
createdb loja_pedidos -U postgres

# (ou se usar pgAdmin: CREATE DATABASE loja_pedidos;)
```

### Opção B: Docker (Recomendado)

```bash
docker run --name postgres-loja \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=loja_pedidos \
  -p 5432:5432 \
  -d postgres:18
```

## Passo 2: Configurar Variáveis de Ambiente

### Linux/Mac

```bash
export DB_URL="jdbc:postgresql://localhost:5432/loja_pedidos"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export TRANSLOG_URL="https://api.translog.com.br"
```

### Windows (PowerShell)

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/loja_pedidos"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
$env:TRANSLOG_URL = "https://api.translog.com.br"
```

### Windows (CMD)

```cmd
set DB_URL=jdbc:postgresql://localhost:5432/loja_pedidos
set DB_USER=postgres
set DB_PASSWORD=postgres
set TRANSLOG_URL=https://api.translog.com.br
```

## Passo 3: Build e Execução

### Opção A: Executar com Spring Boot Maven

```bash
cd /workspace
mvn spring-boot:run
```

O serviço estará em: `http://localhost:8080`

### Opção B: Build JAR e Executar

```bash
cd /workspace
mvn clean package -DskipTests
java -jar target/pedidos-1.0.0.jar
```

### Opção C: Apenas Verificar Compilação

```bash
mvn verify
```

(Isso não requer banco de dados)

## Passo 4: Testar a API

### Health Check

```bash
curl http://localhost:8080/
```

### Exemplo 1: Obter um Produto

```bash
curl http://localhost:8080/produtos/CAM-001 \
  -H "Content-Type: application/json"
```

Resposta:
```json
{
  "sku": "CAM-001",
  "nome": "Camiseta básica",
  "preco": "79.90",
  "pesoKg": "0.30",
  "estoque": 50,
  "foraDeLinha": false
}
```

### Exemplo 2: Calcular Resumo de Compra

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": {
      "cpf": "12345678901",
      "email": "cliente@example.com"
    },
    "itens": [
      { "sku": "CAM-001", "quantidade": 2 },
      { "sku": "MEI-001", "quantidade": 3 }
    ],
    "cep": "01310100",
    "regiao": "SUDESTE",
    "modalidadeEntrega": "RETIRADA_LOJA",
    "cupom": null,
    "formaPagamento": "PIX",
    "parcelas": 1,
    "nivelClube": "BRONZE"
  }'
```

Resposta:
```json
{
  "subtotalProdutos": "199.70",
  "descontoCupom": "0.00",
  "frete": "0.00",
  "prazoEntregaDias": 1,
  "seguro": "2.00",
  "ajustePagamento": "-9.99",
  "totalFinal": "191.71",
  "parcelas": 1,
  "valorParcela": "191.71",
  "creditoProximaCompra": "0.00",
  "brinde": false
}
```

### Exemplo 3: Registrar um Pedido

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": {
      "cpf": "98765432101",
      "email": "novo@example.com"
    },
    "itens": [
      { "sku": "MEI-001", "quantidade": 2 }
    ],
    "cep": "01310100",
    "regiao": "SUDESTE",
    "modalidadeEntrega": "RETIRADA_LOJA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "CARTAO",
    "parcelas": 2,
    "nivelClube": "BRONZE"
  }'
```

Resposta (status 201):
```json
{
  "id": 1,
  "situacao": "AGUARDANDO_PAGAMENTO",
  "criadoEm": "2026-10-14T21:05:00Z",
  "resumo": { ... },
  "itens": [ ... ],
  "codigoRastreio": ""
}
```

### Exemplo 4: Registrar Pagamento

```bash
curl -X POST http://localhost:8080/pedidos/1/pagamento \
  -H "Content-Type: application/json" \
  -d '{
    "idTransacao": "TX-001",
    "pagoEm": "2026-10-14T21:10:00Z"
  }'
```

Resposta:
```json
{
  "id": 1,
  "situacao": "PAGO",
  "criadoEm": "2026-10-14T21:05:00Z",
  "resumo": { ... },
  "itens": [ ... ],
  "codigoRastreio": ""
}
```

### Exemplo 5: Enviar Pedido

```bash
curl -X POST http://localhost:8080/pedidos/1/envio \
  -H "Content-Type: application/json" \
  -d '{
    "codigoRastreio": "BR123456789ABCD"
  }'
```

Resposta:
```json
{
  "id": 1,
  "situacao": "ENVIADO",
  "criadoEm": "2026-10-14T21:05:00Z",
  "resumo": { ... },
  "itens": [ ... ],
  "codigoRastreio": "BR123456789ABCD"
}
```

### Exemplo 6: Consultar Cliente

```bash
curl http://localhost:8080/clientes/98765432101
```

Resposta:
```json
{
  "cpf": "98765432101",
  "saldoCredito": "0.00"
}
```

### Exemplo 7: Consultar Avisos

```bash
curl http://localhost:8080/clientes/98765432101/avisos
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

## Troubleshooting

### Erro: "Cannot connect to database"

**Solução**: Certifique-se que:
1. PostgreSQL está rodando: `psql -U postgres`
2. Banco existe: `\l` (em psql)
3. Variáveis de ambiente estão corretas

### Erro: "Port 8080 already in use"

**Solução**: Mude a porta:
```bash
java -jar target/pedidos-1.0.0.jar --server.port=8081
```

### Erro: "OutOfMemory"

**Solução**: Aumente heap:
```bash
export MAVEN_OPTS="-Xmx1024m"
mvn spring-boot:run
```

### Erro ao conectar com TransLog

**Solução**: Se offline:
1. O serviço retorna erro 503: `TRANSPORTADORA_INDISPONIVEL`
2. Para testes, use sempre `RETIRADA_LOJA` (não precisa TransLog)

## Parar o Serviço

```bash
# Ctrl+C no terminal, ou:
killall java
```

## Logs

O serviço não mostra logs SQL por padrão. Para ativar:

```bash
export SPRING_JPA_SHOW_SQL=true
mvn spring-boot:run
```

## Backup do Banco

```bash
pg_dump loja_pedidos > backup.sql
```

## Reset do Banco

```bash
dropdb loja_pedidos
createdb loja_pedidos
# Ao rodar o serviço, Flyway recriar o schema
```

## Próximos Passos

1. **Explorar a API**: Experimente todos os endpoints
2. **Ler a documentação**:
   - `README.md`: Overview
   - `ARQUITETURA.md`: Design interno
   - `CHECKLIST_IMPLEMENTACAO.md`: Funcionalidades
3. **Fazer alterações**: Customize conforme necessário
4. **Deploy**: Coloque em servidor de produção

## Suporte

Se encontrar problemas:
1. Verifique os logs: `mvn spring-boot:run`
2. Consulte `ARQUITETURA.md` para entender o design
3. Revise os testes: `src/test/`
