# Guia Rápido - Pousada Maré Alta

## Início Rápido

### 1. Compilar o projeto

```bash
mvn clean package
```

### 2. Iniciar PostgreSQL (exemplo com Docker)

```bash
docker run -d \
  --name pousada-db \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18-alpine
```

### 3. Executar a aplicação

```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta 7070: http://localhost:7070

### 4. Testar a API

```bash
# Listar quartos
curl http://localhost:7070/rooms | jq .

# Criar uma reserva
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
  }' | jq .
```

### 5. Executar exemplos completos

```bash
./test-examples.sh
```

## Exemplos de Requisições

### Reserva Normal (STANDARD)
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```
**Resultado**: 2 noites × R$ 249,90 = R$ 499,80

### Reserva Não Reembolsável (NON_REFUNDABLE)
```json
{
  "roomNumber": "201",
  "guestName": "Maria Santos",
  "guestEmail": "maria@example.com",
  "guestCpf": "98765432109",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE"
}
```
**Resultado**: 
- Diárias: R$ 779,80
- Desconto (15%): R$ 116,97
- Total: R$ 662,83

### Reserva Corporativa (CORPORATE)
```json
{
  "roomNumber": "201",
  "guestName": "Ana Costa",
  "guestEmail": "ana@example.com",
  "guestCpf": "55566677788",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL"
}
```
**Resultado**: 
- Diárias: R$ 779,80
- Desconto TECNOSUL (12%): R$ 93,58
- Total: R$ 686,22

### Pacote de Fim de Semana (WEEKEND_PACKAGE)
```json
{
  "roomNumber": "201",
  "guestName": "Pedro Souza",
  "guestEmail": "pedro@example.com",
  "guestCpf": "99988877766",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 3,
  "ratePlan": "WEEKEND_PACKAGE"
}
```
**Resultado**: Preço fixo R$ 820,00 (sexta a domingo)

### Estadia Longa (LONG_STAY)
```json
{
  "roomNumber": "101",
  "guestName": "Lucia Ferreira",
  "guestEmail": "lucia@example.com",
  "guestCpf": "44455566677",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "guests": 2,
  "ratePlan": "LONG_STAY"
}
```
**Resultado**: 
- Diárias: R$ 1.749,30 (7 noites)
- Desconto (10%): R$ 174,93
- Total: R$ 1.574,37

### Alta Temporada
```json
{
  "roomNumber": "101",
  "guestName": "Carlos Lima",
  "guestEmail": "carlos@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 1,
  "ratePlan": "STANDARD"
}
```
**Resultado**: 2 noites × R$ 299,88 = R$ 599,76 (com acréscimo de 20%)

## Fluxo Completo de Reserva

```bash
# 1. Criar reserva
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber":"101","guestName":"João Silva","guestEmail":"joao@example.com","guestCpf":"12345678901","checkIn":"2027-03-10","checkOut":"2027-03-12","guests":2,"ratePlan":"STANDARD"}')
ID=$(echo $RESERVATION | jq -r .id)

# 2. Confirmar reserva (paga e envia e-mail)
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation | jq .

# 3. Check-in
curl -s -X POST http://localhost:7070/reservations/$ID/check-in | jq .

# 4. Check-out (recebe comprovante)
curl -s -X POST http://localhost:7070/reservations/$ID/check-out | jq .

# 5. Segunda via do comprovante
curl -s http://localhost:7070/reservations/$ID/document | jq .
```

## Relatórios

```bash
# Ocupação de uma data
curl http://localhost:7070/reports/occupancy?date=2027-03-10

# Financeiro de um mês
curl http://localhost:7070/reports/revenue?month=2027-03
```

## Códigos de Empresa (CORPORATE)

- **TECNOSUL**: 12% de desconto
- **AGROVALE**: 18% de desconto
- **FARMAVIDA**: 25% de desconto

## Regras Importantes

✅ **Permitido:**
- Reservar quartos disponíveis respeitando a capacidade
- Cancelar antes do check-in
- Confirmar apenas reservas PENDING
- Check-in apenas em reservas CONFIRMED
- Check-out apenas em reservas CHECKED_IN

❌ **Não Permitido:**
- Reservar quarto já ocupado nas mesmas datas
- Mais hóspedes que a capacidade do quarto
- Menos de 1 hóspede
- Data de saída antes ou igual à entrada
- WEEKEND_PACKAGE fora de sexta a domingo
- LONG_STAY com menos de 7 noites
- CORPORATE sem código de empresa válido

## Variáveis de Ambiente

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```
