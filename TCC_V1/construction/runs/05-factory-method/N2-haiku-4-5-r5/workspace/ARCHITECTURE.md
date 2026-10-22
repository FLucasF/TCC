# Arquitetura do Sistema de Reservas - Pousada Maré Alta

## Visão Geral

O sistema é uma API REST construída com **Java 21**, **Javalin** e **PostgreSQL**, implementando um gerenciador completo de reservas de hotel com suporte a múltiplos tipos de documentos e clientes.

## Estrutura de Pacotes

```
com.pousadamareaalta/
├── Main.java                          # Ponto de entrada da aplicação
├── Database.java                      # Gerenciador de conexão e schema
├── documents/                         # Geração de documentos
│   ├── DocumentGenerator.java         # Interface Strategy
│   ├── CpfDocument.java              # Comprovante em português
│   ├── CnpjDocument.java             # Fatura com ISS retido
│   ├── PassportDocument.java         # Booking receipt em inglês
│   └── DocumentGeneratorFactory.java # Factory para criar geradores
├── handlers/                          # Controladores HTTP
│   ├── RoomHandler.java
│   ├── ReservationHandler.java       # Lógica principal de reservas
│   ├── MessageHandler.java
│   └── ReportHandler.java
├── models/                            # Entidades de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── Document.java
├── repositories/                      # Acesso a dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
└── utils/
    └── PriceCalculator.java          # Cálculos de tarifa e arredondamento
```

## Padrões de Design

### Strategy Pattern (Documentos)

O sistema usa o padrão **Strategy** para gerar diferentes tipos de documentos sem duplicar código:

```
DocumentGenerator (interface)
├── CpfDocument
├── CnpjDocument
└── PassportDocument
```

Cada estratégia implementa sua própria formatação, idioma e layout. A `DocumentGeneratorFactory` decide qual estratégia usar baseado no tipo de identificação da reserva.

**Vantagem**: Adicionar novos tipos de documentos (ex: agências de turismo) requer apenas:
1. Criar uma nova classe implementando `DocumentGenerator`
2. Registrar na `DocumentGeneratorFactory`

Nenhum outro código precisa mudar.

## Fluxo de Reserva

```
1. Criar Reserva (POST /reservations)
   └─ Status: PENDING
   └─ amountPaid: 0

2. Confirmar (POST /reservations/{id}/confirmation)
   └─ Status: CONFIRMED
   └─ amountPaid: total
   └─ Envia e-mail com documento

3. Check-in (POST /reservations/{id}/check-in)
   └─ Status: CHECKED_IN

4. Check-out (POST /reservations/{id}/check-out)
   └─ Status: CHECKED_OUT
   └─ Retorna documento (segunda via)

5. Cancelar (pode ser em PENDING ou CONFIRMED)
   └─ Status: CANCELLED
   └─ refund: depende do plano de tarifa
      ├─ STANDARD: refund = amountPaid (100%)
      └─ NON_REFUNDABLE: refund = 0 (sem reembolso)
```

## Cálculo de Preços

O sistema implementa arredondamento **banker's rounding** (HALF_EVEN) em todas as operações:

1. **Preço por noite** (arredondado)
   - Normal: `baseRate`
   - Alta temporada (15 dez-28 fev + julho): `baseRate * 1.20`

2. **Subtotal** (soma das noites, arredondado)

3. **Desconto** (se NON_REFUNDABLE)
   - `subtotal * 0.15` (15% de desconto)

4. **Total** (arredondado)
   - `subtotal - discount`

## Tipos de Identificação de Hóspedes

### CPF (Pessoa Física)
- Campo: `guestCpf`
- Documento: Comprovante de reserva (português)
- Formato no comprovante: "CPF 111.222.333-44"

### CNPJ (Empresa)
- Campos: `companyCnpj`, `companyName`
- Documento: Fatura de hospedagem com ISS retido 5%
- Formato no comprovante: "CNPJ 12.345.678/0001-90"

### Passaporte (Estrangeiro)
- Campos: `guestPassport`, `guestCountry`
- Documento: Booking receipt em inglês
- Formato no comprovante: "passport X1234567, United Kingdom"

## Relatórios

### Ocupação (GET /reports/occupancy?date=2027-07-10)
- CSV com reservas confirmadas/checked-in/checked-out naquela noite
- "Quem entra naquela noite conta, quem sai naquele dia não"
- Ordenado por número do quarto

### Receita (GET /reports/revenue?month=2027-07)
- CSV com reservas que começam naquele mês e deixaram dinheiro
- Mostra: amount_paid - refund (negativo se reembolsado)
- Uma reserva NON_REFUNDABLE cancelada continua contando

## Validações

- ✓ Datas em formato válido (yyyy-MM-dd)
- ✓ Data de saída > data de entrada
- ✓ Hóspedes: min 1, max capacidade do quarto
- ✓ Quarto existe
- ✓ Quarto disponível no período (sem sobreposição)
- ✓ Exatamente um tipo de identificação (CPF OR CNPJ+nome OR Passaporte+país)

## Segurança e Confiabilidade

- Todas as conexões com banco em try-with-resources
- Transações automáticas do HikariCP
- Validação de entrada em todos os endpoints
- Arredondamento banker's rounding para consistência monetária

## Extensibilidade

Para adicionar um novo tipo de documento de hospedagem (ex: agências de turismo):

1. Criar `TourAgencyDocument implements DocumentGenerator`
2. Implementar `getTitle()` e `generate()`
3. Adicionar campos de identificação em `Reservation`
4. Registrar na `DocumentGeneratorFactory`

Nenhuma outra mudança no código!
