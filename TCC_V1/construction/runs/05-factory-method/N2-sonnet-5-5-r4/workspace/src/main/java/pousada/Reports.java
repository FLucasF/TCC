package pousada;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

class Reports {
    private final Database db;
    private final Repository repository;

    Reports(Database db, Repository repository) {
        this.db = db;
        this.repository = repository;
    }

    String occupancy(String dateParam) {
        LocalDate night;
        try {
            night = LocalDate.parse(dateParam);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid("Data inválida; use o formato 2027-07-10.");
        }
        return db.transaction(c -> {
            List<Reservation> occupied = repository.paidOccupying(c, night);
            int totalRooms = repository.rooms(c).size();
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupied) {
                csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut())
                        .append('\n');
            }
            return csv.append("total,").append(occupied.size()).append('/').append(totalRooms).append('\n')
                    .toString();
        });
    }

    String revenue(String monthParam) {
        YearMonth month;
        try {
            month = YearMonth.parse(monthParam);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid("Mês inválido; use o formato 2027-07.");
        }
        return db.transaction(c -> {
            List<Reservation> list = repository.startingBetween(c, month.atDay(1), month.atEndOfMonth());
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.round(BigDecimal.ZERO);
            for (Reservation r : list) {
                csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn())
                        .append(',').append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(r.retained().toPlainString()).append('\n');
                sum = sum.add(r.retained());
            }
            return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
        });
    }
}
