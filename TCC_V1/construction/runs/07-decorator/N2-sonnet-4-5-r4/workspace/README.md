# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL do banco PostgreSQL (formato: `jdbc:postgresql://host:port/database`)
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `PORT` (opcional): Porta do servidor (padrão: 7070)
- `HOTEL_TEST_MODE` (opcional): Define modo de teste (`true` ou `false`)

## Build e Execução

```bash
# Compilar e executar testes
mvn verify

# Gerar o JAR
mvn package

# Executar o sistema
java -jar target/reservations-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Mês de julho completo
- Acréscimo de 20% na diária

### Planos de Tarifa
- STANDARD: reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável

### Sistema de Mensagens

O sistema aplica regras às mensagens antes de enviá-las:

1. **Validação de e-mail**: Bloqueia mensagens com endereços inválidos
2. **Cópia para arquivo**: Gera cópia para arquivo@pousadamarealta.com.br (exceto em modo de teste)
3. **Modo de teste**: Redireciona para testes@pousadamarealta.com.br com prefixo [TESTE]

Novas regras podem ser adicionadas facilmente através do padrão Chain of Responsibility.
