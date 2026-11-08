# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Características

- **Gerenciamento de Quartos**: 5 quartos com tipos (STANDARD, DELUXE, SUITE) e capacidades diferentes
- **Sistema de Reservas**: Criação, confirmação, check-in, check-out e cancelamento de reservas
- **Cálculo de Preços**: Arredondamento "meio para o par", taxa de alta temporada (+20%), planos de tarifa (STANDARD, NON_REFUNDABLE com -15%)
- **Emissão de Comprovantes**: Geração e armazenamento de comprovantes de reserva
- **Notificações**: Sistema de emails (armazenados em banco de dados)
- **Relatórios**: 4 relatórios em CSV (ocupação, receita, taxa de turismo, análise de planos)

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Instalação

1. Clone o repositório
2. Configure as variáveis de ambiente:
   - `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
   - `DB_USER`: Usuário do banco de dados
   - `DB_PASSWORD`: Senha do banco de dados
   - `PORT`: Porta para o servidor (padrão: 7070)

## Build e Execução

```bash
# Build
mvn clean package

# Executar
java -jar target/pousada-sistema-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por data
- `GET /reports/revenue?month=2027-07` - Receita por mês
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo por mês
- `GET /reports/rate-plans?month=2027-07` - Análise de planos de tarifa

## Testes

```bash
mvn test
mvn verify
```

## Estrutura do Projeto

```
src/main/java/com/marealata/
  ├── App.java                 # Entrada da aplicação
  ├── database/                # Configuração do banco de dados
  ├── model/                   # Modelos de dados
  ├── dao/                     # Data Access Objects
  ├── service/                 # Lógica de negócio
  ├── handler/                 # Handlers dos endpoints
  └── util/                    # Utilitários (preço, formatação)
```

## Notas Técnicas

- Arredondamento: Usa `RoundingMode.HALF_EVEN` (banker's rounding)
- Alta Temporada: 15 de dezembro a fim de fevereiro + mês de julho (+20% nas diárias)
- Cancelamento: STANDARD retorna o dinheiro pago, NON_REFUNDABLE não retorna
- Relatórios: Exportáveis em CSV com charset UTF-8
