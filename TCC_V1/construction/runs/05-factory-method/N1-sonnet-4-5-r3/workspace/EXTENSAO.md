# Como Adicionar Novos Tipos de Identificação

O sistema foi organizado seguindo o CLAUDE.md para que novos tipos de identificação possam ser adicionados sem mexer no código existente.

## Exemplo: Agência de Turismo

Para adicionar suporte a reservas feitas por agências de turismo, você precisa:

### 1. Criar a nova classe de identificação

```java
// src/main/java/com/pousadamareal/model/AgencyIdentification.java
package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AgencyIdentification implements GuestIdentification {
    private final String agencyCnpj;
    private final String agencyName;
    private final String bookingReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AgencyIdentification(String agencyCnpj, String agencyName, String bookingReference) {
        this.agencyCnpj = agencyCnpj;
        this.agencyName = agencyName;
        this.bookingReference = bookingReference;
    }

    @Override
    public String getGuestCpf() { return null; }

    @Override
    public String getCompanyCnpj() { return null; }

    @Override
    public String getCompanyName() { return null; }

    @Override
    public String getGuestPassport() { return null; }

    @Override
    public String getGuestCountry() { return null; }
    
    public String getAgencyCnpj() { return agencyCnpj; }
    public String getAgencyName() { return agencyName; }
    public String getBookingReference() { return bookingReference; }

    @Override
    public Document generateDocument(long reservationId, String guestName, String roomNumber,
                                    RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                                    int nights, BigDecimal subtotal, BigDecimal discount,
                                    BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append("Voucher de reserva nº ").append(reservationId).append("\n");
        text.append("Agência: ").append(agencyName).append(" (CNPJ ").append(agencyCnpj).append(")\n");
        text.append("Referência: ").append(bookingReference).append("\n");
        text.append("Hóspede: ").append(guestName).append("\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(checkIn.format(DATE_FORMATTER))
            .append(" | Saída: ").append(checkOut.format(DATE_FORMATTER))
            .append(" | ").append(nights == 1 ? "1 diária" : nights + " diárias").append("\n");
        text.append("Diárias: ").append(formatMoney(subtotal)).append("\n");
        
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }
        
        text.append("Total: ").append(formatMoney(total)).append("\n");
        text.append("Comissão da agência (10%): ").append(formatMoney(total.multiply(new BigDecimal("0.10"))));

        return new Document(
            "Voucher de reserva",
            reservationId,
            guestName,
            null, null, null, null, null,
            roomNumber,
            roomType,
            checkIn,
            checkOut,
            nights,
            subtotal,
            discount,
            total,
            text.toString()
        );
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
```

### 2. Atualizar o banco de dados

Adicione as colunas em `Database.java`:

```java
agency_cnpj VARCHAR(20),
agency_name VARCHAR(255),
booking_reference VARCHAR(100),
```

### 3. Atualizar o ReservationRepository

No método `buildReservation`, adicione:

```java
} else if (agencyCnpj != null) {
    identification = new AgencyIdentification(
        agencyCnpj, 
        rs.getString("agency_name"),
        rs.getString("booking_reference")
    );
}
```

### 4. Atualizar o CreateReservationRequest

Adicione os campos:

```java
private String agencyCnpj;
private String agencyName;
private String bookingReference;
```

### 5. Atualizar o ReservationController

Adicione o caso no `createReservation`:

```java
} else if (request.getAgencyCnpj() != null && 
           request.getAgencyName() != null && 
           request.getBookingReference() != null) {
    identification = new AgencyIdentification(
        request.getAgencyCnpj(),
        request.getAgencyName(),
        request.getBookingReference()
    );
    identificationCount++;
}
```

## Vantagens desta Organização

1. **Não mexe no que já funciona**: As classes `CpfIdentification`, `CnpjIdentification` e `PassportIdentification` não precisam ser alteradas.

2. **Um lugar só**: Toda a lógica do novo tipo (formatação, cálculos específicos, campos) fica em uma única classe.

3. **Fácil de testar**: Cada tipo pode ser testado isoladamente.

4. **Fácil de entender**: Alguém procurando "como funciona o documento de agência" vai direto para `AgencyIdentification.java`.

5. **Pronto para mais**: Cartão de crédito corporativo, convênio com empresa, programa de fidelidade... cada novo tipo segue o mesmo padrão.
