package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

public final class ReportService {
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();

    public ReportService(Database db) {
        this.db = db;
    }

    public String occupancy(String dateParam) {
        LocalDate night = parseDate(dateParam);
        List<Reservation> occupying = db.read(c -> reservations.paidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return csv.append("total,").append(rooms).append('/').append(Rooms.all().size()).append('\n').toString();
    }

    public String revenue(String monthParam) {
        YearMonth month = parseMonth(monthParam);
        List<Reservation> kept = db.read(c ->
                reservations.keptMoney(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(r.kept()).append('\n');
            sum = sum.add(r.kept());
        }
        return csv.append("total,").append(sum).append('\n').toString();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            throw ApiError.invalidRequest("Data fora do formato AAAA-MM-DD");
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || !MONTH.matcher(value).matches()) {
            throw ApiError.invalidRequest("Mês fora do formato AAAA-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (RuntimeException e) {
            throw ApiError.invalidRequest("Mês fora do formato AAAA-MM");
        }
    }
}
