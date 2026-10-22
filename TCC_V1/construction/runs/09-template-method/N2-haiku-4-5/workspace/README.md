# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Funcionalidades

### Quartos
- 5 quartos: 2 STANDARD, 2 DELUXE, 1 SUITE
- Capacidades de 2 a 4 hóspedes
- Diárias de R$ 249,90 a R$ 579,90

### Reservas
- Criação de reservas com validações
- Fluxo de confirmação, check-in e check-out
- Cancelamento com cálculo automático de reembolsos
- Dois planos tarifários: STANDARD (preço cheio) e NON_REFUNDABLE (15% desconto)
- Alta temporada: 15 de dezembro a 28 de fevereiro + julho completo (+20% na diária)

### Comprovantes
- Geração automática de comprovantes de reserva
- Envio de e-mail de confirmação (armazenado para consulta)
- Segunda via disponível para reservas pagas

### Relatórios (em CSV)
1. **Ocupação**: Quartos ocupados em uma data específica
2. **Financeiro**: Receita por mês (reservas pagas)
3. **Taxa de Turismo** (NOVO): R$ 4,50 por hóspede por noite
4. **Planos Tarifários** (NOVO): Análise de receita por plano

Todos os relatórios incluem cabeçalho com nome, tipo de relatório e período.

## Como Executar

### Pré-requisitos
- Java 21
- PostgreSQL 18
- Maven 3.8+

### Compilação
```bash
mvn clean package
```

Gera um JAR executável em `target/app.jar`.

### Execução
```bash
java -jar target/app.jar
```

As variáveis de ambiente são obrigatórias:
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=7070 \
java -jar target/app.jar
```

Se `PORT` não for definida, a aplicação usa a porta 7070.

### Testes
```bash
mvn verify
```

Executa testes unitários de validação e lógica.

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva (status PENDING)
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma e paga a reserva
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todos os e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação em uma data
- `GET /reports/revenue?month=2027-07` - Receita de um mês
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo de um mês
- `GET /reports/rate-plans?month=2027-07` - Análise por plano tarifário

## Exemplo de Uso

### Criar uma Reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar a Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'
```

### Gerar Relatório de Ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

## Arquitetura

```
com.pousada/
├── Application.java          # Entry point
├── api/                      # Routes (Javalin)
├── domain/                   # Modelos de domínio
├── repo/                     # Acesso ao banco
├── service/                  # Lógica de negócio
│   ├── ReservationService
│   └── report/               # Template Method para relatórios
└── util/                     # Utilitários
    ├── Database              # Inicialização e conexão
    ├── Money                 # Arredondamento HALF_EVEN
    └── DateUtil              # Datas e alta temporada
```

## Tratamento de Erros

| Situação | Status | Erro |
|----------|--------|------|
| Campo faltando | 400 | INVALID_REQUEST |
| Quarto não existe | 404 | ROOM_NOT_FOUND |
| Reserva não existe | 404 | RESERVATION_NOT_FOUND |
| Quarto indisponível | 409 | ROOM_UNAVAILABLE |
| Operação não permitida | 409 | OPERATION_NOT_ALLOWED |
| Capacidade excedida | 422 | CAPACITY_EXCEEDED |

## Banco de Dados

Criado automaticamente na primeira execução com as tabelas:
- `rooms`: Configuração dos 5 quartos
- `reservations`: Histórico completo com todas as transições
- `sent_messages`: E-mails enviados para auditoria
- Índices para performance em queries de ocupação

## Observações

- Arredondamento bancário (HALF_EVEN) em todos os valores
- Datas em formato ISO (2027-07-10) na API e relatórios
- Datas formatadas como DD/MM/YYYY em comprovantes
- Valores em CSV como 599.76 (ponto decimal)
- E-mails simulados, armazenados no banco para consulta

## Escalabilidade

O sistema foi projetado para adicionar:
- Novos planos tarifários (sem alterar código existente)
- Novos relatórios (baseado em Template Method)
- Novos tipos de mensagem (EMAIL, SMS, WhatsApp)
- Taxa de turismo variável por ano
- Cabeçalhos expandidos em relatórios (data de geração, etc)

Cada novo caso entra em um lugar só, sem modificar o que já funciona.
