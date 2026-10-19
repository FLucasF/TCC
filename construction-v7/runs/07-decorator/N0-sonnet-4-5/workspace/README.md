# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas desenvolvido em Java 21 com Javalin.

## Compilar e executar

### Compilar o projeto
```bash
mvn clean package
```

### Executar os testes
```bash
mvn verify
```

### Iniciar o servidor
```bash
java -jar target/reservas-1.0-SNAPSHOT.jar
```

O servidor inicia na porta 7070 por padrão. Para usar outra porta:
```bash
PORT=8080 java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Modo de teste

Para ativar o modo de teste (e-mails redirecionados):
```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0-SNAPSHOT.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Regras de e-mail

### 1. Validação de e-mail
E-mails inválidos são bloqueados. Um e-mail válido deve:
- Ter um único @
- Ter conteúdo antes do @
- Ter um domínio após o @ com pelo menos um ponto

**Exemplo bloqueado:** ana.gmail.com, ana@gmail

### 2. Cópia para arquivo
Toda mensagem enviada gera uma cópia para `arquivo@pousadamarealta.com.br` com assunto `[Cópia] ...`

### 3. Modo de teste
Com `HOTEL_TEST_MODE=true`:
- Mensagens vão para `testes@pousadamarealta.com.br`
- Assunto começa com `[TESTE] `
- Primeira linha do corpo: `Destinatário original: {email}`
- Não gera cópia para arquivo
- Validação continua funcionando

## Estrutura do projeto

```
src/
├── main/java/com/pousadamarealta/
│   ├── Application.java          # Ponto de entrada
│   ├── controller/                # Controllers REST
│   ├── dto/                       # Data Transfer Objects
│   ├── model/                     # Modelos de domínio
│   ├── service/                   # Lógica de negócio
│   └── util/                      # Utilitários
└── test/java/com/pousadamarealta/
    └── IntegrationTest.java       # Testes de integração
```

## Quartos disponíveis

| Número | Tipo     | Capacidade | Diária    |
|--------|----------|------------|-----------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90 |
| 102    | STANDARD | 2 pessoas  | R$ 249,90 |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 301    | SUITE    | 4 pessoas  | R$ 579,90 |

## Alta temporada

A diária tem acréscimo de 20% durante:
- 15 de dezembro a 28 de fevereiro
- Todo o mês de julho

## Planos de tarifa

- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável
