package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.roomNumber(),
                reservation.roomType().name(),
                reservation.id(),
                reservation.checkIn().format(DATE_FORMAT),
                reservation.checkOut().format(DATE_FORMAT)
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.amountPaid().subtract(
                reservation.refund() != null ? reservation.refund() : BigDecimal.ZERO
            );

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%.2f\n",
                    reservation.id(),
                    reservation.roomNumber(),
                    reservation.checkIn().format(DATE_FORMAT),
                    reservation.checkOut().format(DATE_FORMAT),
                    reservation.status().name(),
                    revenue
                ));
                total = total.add(revenue);
            }
        }

        csv.append(String.format("total,%.2f\n", total));

        return csv.toString();
    }
}
