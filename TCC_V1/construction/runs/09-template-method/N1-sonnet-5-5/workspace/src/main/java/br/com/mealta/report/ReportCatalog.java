package br.com.mealta.report;

import br.com.mealta.domain.ErrorCode;
import br.com.mealta.domain.PousadaException;
import br.com.mealta.domain.RoomCatalog;
import br.com.mealta.store.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Todos os relatórios da pousada. Um relatório novo entra como mais um item de {@link #definitions()}. */
public class ReportCatalog {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;
    private final BigDecimal tourismTaxPerGuestNight;

    public ReportCatalog(ReservationRepository reservations, RoomCatalog rooms,
                         BigDecimal tourismTaxPerGuestNight) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.tourismTaxPerGuestNight = tourismTaxPerGuestNight;
    }

    public List<ReportDefinition> definitions() {
        return List.of(
                new ReportDefinition("occupancy", "date", raw -> {
                    LocalDate night = date(raw);
                    return new OccupancyReport(night, reservations.paidOccupying(night), rooms);
                }),
                new ReportDefinition("revenue", "month", raw -> {
                    YearMonth month = month(raw);
                    return new RevenueReport(month, reservations.startingIn(month));
                }),
                new ReportDefinition("tourism-tax", "month", raw -> {
                    YearMonth month = month(raw);
                    return new TourismTaxReport(month, tourismTaxPerGuestNight, reservations.startingIn(month));
                }),
                new ReportDefinition("rate-plans", "month", raw -> {
                    YearMonth month = month(raw);
                    return new RatePlanReport(month, reservations.startingIn(month));
                }));
    }

    private static LocalDate date(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "Data inválida, use AAAA-MM-DD");
        }
    }

    private static YearMonth month(String raw) {
        try {
            if (raw == null || !raw.matches("\\d{4}-\\d{2}")) {
                throw new DateTimeParseException("formato", String.valueOf(raw), 0);
            }
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "Mês inválido, use AAAA-MM");
        }
    }
}
