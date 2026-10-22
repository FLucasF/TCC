package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                r.getRoomType(),
                r.getId(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER)));
        }

        csv.append(String.format("total,%d/%d", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO);
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format(Locale.US, "%d,%s,%s,%s,%s,%.2f\n",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER),
                    r.getStatus(),
                    revenue));
                total = total.add(revenue);
            }
        }

        csv.append(String.format(Locale.US, "total,%.2f", total));

        return csv.toString();
    }
}
