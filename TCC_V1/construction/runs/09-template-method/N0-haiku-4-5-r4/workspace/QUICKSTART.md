# Quick Start - Pousada Maré Alta

## Preparação do Banco de Dados

1. Instale PostgreSQL 18
2. Crie um banco de dados para a pousada:
```bash
createdb pousada_marealata
```

## Executar o Sistema

1. Build do projeto:
```bash
mvn clean package
```

2. Execute com as variáveis de ambiente:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_marealata"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"

java -jar target/pousada-sistema-1.0.0.jar
```

O servidor iniciará na porta 7070.

## Testando a API

### 1. Verificar quartos disponíveis
```bash
curl http://localhost:7070/rooms
```

### 2. Criar uma reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### 3. Confirmar a reserva (número 1 neste exemplo)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 599.76
  }'
```

### 4. Verificar email enviado
```bash
curl http://localhost:7070/messages
```

### 5. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 6. Check-out (retorna comprovante)
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### 7. Gerar relatório de ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10" > occupancy.csv
cat occupancy.csv
```

### 8. Gerar relatório financeiro
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07" > revenue.csv
cat revenue.csv
```

### 9. Gerar relatório de taxa de turismo
```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07" > tourism.csv
cat tourism.csv
```

### 10. Gerar relatório de planos
```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07" > rate-plans.csv
cat rate-plans.csv
```

## Estrutura de Dados

### Quartos
- **101**: STANDARD, 2 pessoas, R$ 249,90/noite
- **102**: STANDARD, 2 pessoas, R$ 249,90/noite
- **201**: DELUXE, 3 pessoas, R$ 389,90/noite
- **202**: DELUXE, 3 pessoas, R$ 389,90/noite
- **301**: SUITE, 4 pessoas, R$ 579,90/noite

### Alta Temporada
- 15 de dezembro até 28 de fevereiro
- Todo o mês de julho
- Acréscimo de 20% nas diárias

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolso integral em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

## Solução de Problemas

### Erro de conexão com banco de dados
Verifique:
1. PostgreSQL está rodando
2. Banco de dados foi criado
3. Variáveis de ambiente estão corretas
4. Credenciais de acesso estão corretas

### Porta já em uso
Mude a porta usando:
```bash
export PORT="8080"
```

### Testes falhando
Execute:
```bash
mvn clean test
```

## Estrutura do Projeto

```
pousada-sistema/
├── pom.xml                          # Configuração Maven
├── README.md                        # Documentação principal
├── API_EXAMPLES.md                  # Exemplos de requisições
├── QUICKSTART.md                    # Este arquivo
├── REQUIREMENTS_CHECKLIST.md        # Checklist de requisitos
└── src/
    ├── main/java/com/marealata/
    │   ├── App.java                # Entrada da aplicação
    │   ├── database/               # Configuração do banco
    │   ├── model/                  # Modelos de dados
    │   ├── dao/                    # Data Access Objects
    │   ├── service/                # Lógica de negócio
    │   ├── handler/                # Handlers dos endpoints
    │   └── util/                   # Utilitários
    └── test/java/com/marealata/    # Testes unitários
```

## Próximos Passos

O sistema está pronto para produção! 

Futuras melhorias solicitadas:
- Relatório de cancelamentos
- Relatório de hóspedes por estado
- Taxa de turismo ajustável
- Data de emissão nos relatórios
