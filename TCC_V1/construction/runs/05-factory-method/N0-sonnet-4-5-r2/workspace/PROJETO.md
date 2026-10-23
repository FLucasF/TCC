# Sistema de Reservas - Pousada Maré Alta

## ✅ Sistema Completo e Funcional

O sistema está **totalmente implementado** conforme especificado, pronto para uso.

## 📦 O que foi construído

### 1. Modelo de Domínio
- **5 Quartos**: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- **Alta temporada** com acréscimo de 20%: julho inteiro e 15/12 a 28/02
- **2 Planos de tarifa**: STANDARD e NON_REFUNDABLE (15% desconto)
- **3 Tipos de hóspede**: Pessoa física (CPF), Empresa (CNPJ), Estrangeiro (Passaporte)
- **Ciclo de vida**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)

### 2. Cálculo de Preços
- Arredondamento HALF_EVEN (meio para o par) em cada etapa
- Preço por noite calculado com alta temporada
- Subtotal das noites
- Desconto aplicado (se NON_REFUNDABLE)
- Total = Subtotal - Desconto

### 3. Documentos Gerados
- **Comprovante** (pessoa física): em português com CPF
- **Fatura** (empresa): em português com CNPJ e ISS retido (5%)
- **Booking receipt** (estrangeiro): em inglês com passaporte e país

### 4. E-mails de Confirmação
- Enviados automaticamente ao confirmar reserva
- Armazenados no banco (não enviam de verdade ainda)
- Consultáveis via API `/messages`

### 5. Relatórios CSV
- **Ocupação**: quartos ocupados em determinada noite
- **Financeiro**: receita do mês (pago - reembolsado)

### 6. API REST Completa
11 endpoints implementados conforme especificação:
- GET /rooms
- POST /reservations
- GET /reservations/{id}
- POST /reservations/{id}/confirmation
- POST /reservations/{id}/cancellation
- POST /reservations/{id}/check-in
- POST /reservations/{id}/check-out
- GET /reservations/{id}/document
- GET /messages
- GET /reports/occupancy?date=YYYY-MM-DD
- GET /reports/revenue?month=YYYY-MM

### 7. Validações e Erros
Todos os códigos de erro especificados:
- 400 INVALID_REQUEST
- 404 ROOM_NOT_FOUND
- 404 RESERVATION_NOT_FOUND
- 409 ROOM_UNAVAILABLE
- 409 OPERATION_NOT_ALLOWED
- 422 CAPACITY_EXCEEDED

### 8. Persistência PostgreSQL
- Banco criado automaticamente ao iniciar
- Quartos pré-cadastrados
- HikariCP para pool de conexões

### 9. Testes de Integração
Suite completa de testes verificando:
- Listagem de quartos
- Criação de reservas
- Cálculo de alta temporada
- Aplicação de descontos
- Confirmação e envio de e-mail
- Conflitos de disponibilidade
- Validação de capacidade

## 🏗️ Arquitetura

```
br.com.marealta/
├── Application.java                    # Classe principal
├── controller/                         # Endpoints REST
│   ├── ReservationController.java
│   ├── RoomController.java
│   ├── MessageController.java
│   └── ReportController.java
├── service/                            # Lógica de negócio
│   ├── ReservationService.java
│   ├── PricingService.java
│   ├── DocumentService.java
│   ├── EmailService.java
│   └── ReportService.java
├── repository/                         # Acesso ao banco
│   ├── ReservationRepository.java
│   ├── RoomRepository.java
│   └── MessageRepository.java
├── model/                              # Domínio
│   ├── Reservation.java
│   ├── Room.java
│   ├── Message.java
│   ├── GuestIdentification.java       # Sealed interface (CPF/CNPJ/Passaporte)
│   ├── RoomType.java
│   ├── RatePlan.java
│   └── ReservationStatus.java
├── dto/                                # DTOs da API
│   ├── ReservationDTO.java
│   ├── DocumentDTO.java
│   ├── CreateReservationRequest.java
│   └── ...
├── exception/                          # Exceções de negócio
│   ├── PousadaException.java
│   ├── InvalidRequestException.java
│   ├── RoomNotFoundException.java
│   └── ...
└── database/
    └── DatabaseInitializer.java        # Criação das tabelas
```

## 🚀 Como Usar

### Passo 1: Subir o PostgreSQL

```bash
docker-compose up -d
```

Isso cria um banco PostgreSQL com:
- Host: localhost:5432
- Database: pousada
- User: pousada
- Password: pousada123

### Passo 2: Compilar

```bash
mvn clean package
```

Gera `target/pousada-marealta-1.0.0.jar` (9.3 MB com todas as dependências)

### Passo 3: Executar

```bash
./run.sh
```

Ou manualmente:
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=pousada123 \
PORT=7070 \
java -jar target/pousada-marealta-1.0.0.jar
```

### Passo 4: Testar

```bash
./examples.sh
```

Ou acesse: http://localhost:7070/rooms

## 📝 Exemplos Práticos

### Criar reserva com CPF
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar e receber e-mail
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Ver e-mails enviados
```bash
curl http://localhost:7070/messages
```

### Relatório de ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Mais exemplos em `API_EXAMPLES.md`

## 🧪 Testes

```bash
# Rodar todos os testes
mvn verify

# Apenas testes unitários
mvn test

# Apenas testes de integração
mvn integration-test
```

**Nota**: Os testes requerem PostgreSQL rodando. Configure `DB_URL`, `DB_USER` e `DB_PASSWORD` se necessário.

## 📊 Tecnologias

- **Java 21** - Linguagem (com sealed interfaces, records)
- **Javalin 6.7.0** - Framework web leve
- **PostgreSQL 18** - Banco de dados
- **HikariCP** - Pool de conexões
- **Jackson** - JSON
- **JUnit 5** - Testes
- **REST Assured** - Testes de API
- **Maven** - Build

## 🎯 Destaques da Implementação

### 1. Sealed Interface para Identificação
```java
sealed interface GuestIdentification permits 
    IndividualGuest,      // CPF
    CorporateGuest,       // CNPJ + razão social
    ForeignGuest {}       // Passaporte + país
```

Garante **type-safety** e permite pattern matching no Java 21.

### 2. Arredondamento Correto
```java
value.setScale(2, RoundingMode.HALF_EVEN)
```

Aplicado em **cada etapa** do cálculo, conforme especificado.

### 3. Alta Temporada Automática
```java
private boolean isHighSeason(LocalDate date) {
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();
    
    if (month == 7) return true;
    if (month == 12 && day >= 15) return true;
    if (month == 1 || month == 2) return true;
    
    return false;
}
```

### 4. Validação de Disponibilidade
```sql
SELECT * FROM reservations
WHERE room_number = ?
AND status NOT IN ('CANCELLED')
AND check_in < ?      -- check-out solicitado
AND check_out > ?     -- check-in solicitado
```

Detecta sobreposição de datas corretamente.

### 5. Documentos Multilíngues
Pattern matching para gerar documento correto:
```java
return switch (guestIdentification) {
    case IndividualGuest ig -> generateComprovante(...);
    case CorporateGuest cg -> generateFatura(...);
    case ForeignGuest fg -> generateBookingReceipt(...);
};
```

## 📄 Arquivos do Projeto

- `pom.xml` - Configuração Maven
- `docker-compose.yml` - PostgreSQL para desenvolvimento
- `run.sh` - Script para executar o sistema
- `examples.sh` - Script com exemplos de todas as APIs
- `README.md` - Documentação principal
- `API_EXAMPLES.md` - Exemplos detalhados de uso
- `PROJETO.md` - Este arquivo (resumo técnico)

## ✨ Pronto para Uso

O sistema está **100% funcional** e atende a **todos os requisitos**:

✅ 5 quartos pré-cadastrados  
✅ Cálculo de alta temporada  
✅ Planos de tarifa (normal e não reembolsável)  
✅ 3 tipos de identificação (CPF, CNPJ, passaporte)  
✅ Ciclo completo de reserva  
✅ Documentos formatados (português e inglês)  
✅ E-mails de confirmação  
✅ Relatórios CSV  
✅ API REST completa  
✅ Validações e tratamento de erros  
✅ Persistência em PostgreSQL  
✅ Testes de integração  
✅ JAR executável  

Para começar a usar, basta seguir os passos em **Como Usar** acima!
