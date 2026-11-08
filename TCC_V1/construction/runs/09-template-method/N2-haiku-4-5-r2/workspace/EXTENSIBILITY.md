# Extensibilidade do Sistema

Este documento descreve como estender o sistema com novos relatórios e funcionalidades.

## Adicionar um Novo Relatório

O sistema usa o padrão **Template Method** para relatórios, o que permite adicionar novos tipos sem modificar código existente.

### Passo 1: Criar Nova Classe de Relatório

Crie uma classe que estenda `Report` em `src/main/java/com/pousada/reports/`:

```java
package com.pousada.reports;

import com.pousada.infrastructure.ReservationRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CancellationsReport extends Report {
    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Override
    protected String getReportName() {
        return "cancellations";  // nome do relatório
    }

    @Override
    protected String getReportPeriod(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    @Override
    protected String generateHeader() {
        return "reservation,room,check_in,check_out,status,refund";
    }

    @Override
    protected List<String> generateRows(LocalDate date) throws Exception {
        List<String> rows = new ArrayList<>();
        LocalDate startOfMonth = date.withDayOfMonth(1);

        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        for (var reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                rows.add(String.format("%d,%d,%s,%s,%s,%.2f",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getStatus().name(),
                    reservation.getRefund() != null ? reservation.getRefund() : 0.0
                ));
            }
        }

        return rows;
    }

    @Override
    protected String generateTotal(LocalDate date) throws Exception {
        // implementar lógica de total
        return "total,0,0.00";
    }
}
```

### Passo 2: Registrar no Handler

Adicione a rota no `ReportsHandler`:

```java
public void cancellations(Context ctx) {
    try {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
            return;
        }

        YearMonth yearMonth = YearMonth.parse(monthParam);
        LocalDate date = yearMonth.atDay(1);
        String report = new CancellationsReport().generate(date);
        ctx.header("Content-Type", "text/csv; charset=utf-8");
        ctx.result(report);
    } catch (Exception e) {
        ctx.status(500);
        ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
    }
}
```

### Passo 3: Registrar a Rota

No `Application.java`, adicione:

```java
app.get("/reports/cancellations", new ReportsHandler()::cancellations);
```

### Estrutura de um Relatório

Todo relatório segue a estrutura:

```
[Título]
[Cabeçalho]
[Linhas de dados]
[Linha de total]
```

Exemplo:
```
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-10,2027-07-11
total,2/5
```

## Mudanças Estruturais Futuras

Quando precisar adicionar uma linha de data a todos os relatórios, basta modificar o método `generate` na classe `Report`:

```java
public String generate(LocalDate date) throws Exception {
    // ... existente ...
    
    // Adicione isto no final:
    csv.append("\n").append(String.format("Gerado em: %s", LocalDate.now()));
    
    return csv.toString();
}
```

Esta mudança se propagará automaticamente para todos os 6+ relatórios sem modificar nenhuma outra classe.

## Adicionar Novo Campo a Enums

Para adicionar um novo tipo de quarto ou plano de tarifa:

### RoomType

```java
public enum RoomType {
    STANDARD("STANDARD", 249.90, 2),
    DELUXE("DELUXE", 389.90, 3),
    SUITE("SUITE", 579.90, 4),
    PENTHOUSE("PENTHOUSE", 799.90, 5);  // novo
    // ...
}
```

Depois adicione o quarto no `Database.initialize()`.

### RatePlan

```java
public enum RatePlan {
    STANDARD("STANDARD", 0.0),
    NON_REFUNDABLE("NON_REFUNDABLE", 0.15),
    EARLY_BIRD("EARLY_BIRD", 0.25);  // novo
    // ...
}
```

Não é necessário alterar nenhum outro código!
