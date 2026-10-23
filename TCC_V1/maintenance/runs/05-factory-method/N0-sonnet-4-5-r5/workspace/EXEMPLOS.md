# Exemplos dos Novos Tipos de Reserva

Este arquivo mostra como usar as três formas de criar reservas no sistema.

## 1. Pessoa Física (CPF)

### Requisição:
```json
{
  "roomNumber": 201,
  "guestName": "Ana Souza",
  "guestEmail": "ana@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Documento gerado (Comprovante de reserva):
```
Comprovante de reserva nº 1
Hóspede: Ana Souza (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
```

## 2. Empresa (CNPJ)

### Requisição:
```json
{
  "roomNumber": 201,
  "guestName": "Maria Silva",
  "guestEmail": "maria@example.com",
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Documento gerado (Fatura de hospedagem):
```
Fatura de hospedagem nº 2
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Maria Silva
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

## 3. Estrangeiro (Passaporte)

### Requisição:
```json
{
  "roomNumber": 201,
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Documento gerado (Booking receipt):
```
Booking receipt no. 3
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80
```

## Validações

O sistema rejeita requisições que:
- Não informam nenhum tipo de identificação
- Informam mais de um tipo de identificação
- Informam CNPJ sem razão social
- Informam passaporte sem país

Erro retornado:
```json
{
  "error": "INVALID_REQUEST",
  "message": "exactly one identification type is required: guestCpf, or (companyCnpj and companyName), or (guestPassport and guestCountry)"
}
```

## Resposta da API

Ao consultar uma reserva confirmada (GET /reservations/{id}), o campo `document` contém o documento completo:

```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Ana Souza",
  "guestEmail": "ana@example.com",
  "guestCpf": "111.222.333-44",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null,
  "document": {
    "title": "Comprovante de reserva",
    "reservationId": 1,
    "guestName": "Ana Souza",
    "guestCpf": "111.222.333-44",
    "companyCnpj": null,
    "companyName": null,
    "guestPassport": null,
    "guestCountry": null,
    "roomNumber": 201,
    "roomType": "DELUXE",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "nights": 2,
    "subtotal": 779.80,
    "discount": 0.00,
    "total": 779.80,
    "text": "Comprovante de reserva nº 1\nHóspede: Ana Souza (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n"
  }
}
```
