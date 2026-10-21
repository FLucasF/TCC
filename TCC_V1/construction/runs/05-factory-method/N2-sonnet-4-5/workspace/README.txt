# Sistema de Reservas - Pousada Maré Alta

## Como compilar

mvn clean package

## Como executar

java -jar target/mare-alta-1.0.0.jar

Por padrão, o servidor inicia na porta 7070. Para usar outra porta:

PORT=8080 java -jar target/mare-alta-1.0.0.jar

## Endpoints disponíveis

### Quartos
GET /rooms - Lista todos os quartos

### Reservas
POST /reservations - Cria uma nova reserva
GET /reservations/{id} - Consulta uma reserva
POST /reservations/{id}/confirmation - Confirma uma reserva
POST /reservations/{id}/cancellation - Cancela uma reserva
POST /reservations/{id}/check-in - Faz check-in
POST /reservations/{id}/check-out - Faz check-out (retorna documento)
GET /reservations/{id}/document - Obtém segunda via do documento

### Mensagens
GET /messages - Lista mensagens enviadas (emails)

### Relatórios
GET /reports/occupancy?date=YYYY-MM-DD - Relatório de ocupação
GET /reports/revenue?month=YYYY-MM - Relatório financeiro

## Tipos de identificação

### Pessoa física (CPF)
{
  "guestCpf": "12345678901"
}

### Empresa (CNPJ)
{
  "companyCnpj": "12345678000190",
  "companyName": "Nome da Empresa"
}

### Estrangeiro (Passaporte)
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}

## Exemplo de criação de reserva

curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
