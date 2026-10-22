package br.com.marealte.service;

import br.com.marealte.model.Reservation;
import br.com.marealte.model.ReservationStatus;
import br.com.marealte.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    private final ReservationRepository reservationRepository;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findOccupiedOnDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(r.getRoomNumber()).append(",");
            csv.append(reservationRepository.getRoomType(r.getRoomNumber())).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(ISO_DATE)).append(",");
            csv.append(r.getCheckOut().format(ISO_DATE)).append("\n");
        }

        int occupiedRooms = reservations.size();
        int totalRooms = 5;
        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getAmountPaid();
            if (r.getRefund() != null) {
                revenue = revenue.subtract(r.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.getId()).append(",");
                csv.append(r.getRoomNumber()).append(",");
                csv.append(r.getCheckIn().format(ISO_DATE)).append(",");
                csv.append(r.getCheckOut().format(ISO_DATE)).append(",");
                csv.append(r.getStatus()).append(",");
                csv.append(revenue.toString()).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(total.toString());

        return csv.toString();
    }
}
