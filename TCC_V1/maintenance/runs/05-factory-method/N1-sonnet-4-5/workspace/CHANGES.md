# Mudanças Implementadas

Sistema agora suporta três tipos de hóspede com documentos específicos para cada um.

## Tipos de Reserva

### 1. Pessoa Física (CPF)
**Request:**
```json
{
  "guestCpf": "111.222.333-44",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  ...
}
```

**Documento (Comprovante de reserva):**
```
Comprovante de reserva nº 1
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
```

### 2. Empresa (CNPJ)
**Request:**
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "guestName": "Ana Souza",
  "guestEmail": "ana@example.com",
  ...
}
```

**Documento (Fatura de hospedagem):**
```
Fatura de hospedagem nº 3
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

### 3. Estrangeiro (Passaporte)
**Request:**
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  ...
}
```

**Documento (Booking receipt):**
```
Booking receipt no. 4
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80
```

## Mudanças no Banco de Dados

Nova migração `V2__add_guest_types.sql`:
- `guest_cpf` agora é opcional (nullable)
- Novas colunas: `company_cnpj`, `company_name`, `guest_passport`, `guest_country`
- Constraint garante que exatamente um tipo de identificação está presente

## Validação da API

- Exatamente um dos três tipos deve ser fornecido:
  - `guestCpf` OU
  - `companyCnpj` + `companyName` OU
  - `guestPassport` + `guestCountry`
- Caso contrário, retorna 400 com `INVALID_REQUEST`

## Response da API

A resposta de `GET /reservations/{id}` agora inclui:
- `guestCpf`, `companyCnpj`, `companyName`, `guestPassport`, `guestCountry`
- Campos não aplicáveis retornam `null`

O documento (`GET /reservations/{id}/document` e check-out) agora inclui:
- Todos os campos de identificação
- Campo `text` com o texto formatado do documento

## Testes

Adicionados testes para:
- Reserva de empresa com fatura
- Reserva de estrangeiro com booking receipt
- Validação de identificação inválida (nenhuma, múltiplas, incompletas)

Todos os 29 testes passam (`mvn verify`).
