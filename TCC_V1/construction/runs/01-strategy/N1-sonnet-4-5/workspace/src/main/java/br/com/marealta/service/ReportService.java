package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getRoomType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getDateRange().checkIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getDateRange().checkOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        Money totalRevenue = Money.zero();

        for (Reservation reservation : reservations) {
            Money revenue = reservation.getRevenue();
            if (revenue.isPositive()) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getDateRange().checkIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getDateRange().checkOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatAmount(totalRevenue));

        return csv.toString();
    }

    private String formatAmount(Money money) {
        return money.amount().toString();
    }
}
