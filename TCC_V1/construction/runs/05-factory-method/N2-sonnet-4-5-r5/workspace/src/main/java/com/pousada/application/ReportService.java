package com.pousada.application;

import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.Room;
import com.pousada.domain.repository.ReservationRepository;
import com.pousada.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(r.roomNumber()).append(",");
            csv.append(getRoomType(r.roomNumber())).append(",");
            csv.append(r.id()).append(",");
            csv.append(r.checkIn().format(DATE_FORMAT)).append(",");
            csv.append(r.checkOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = calculateRevenue(r);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.id()).append(",");
                csv.append(r.roomNumber()).append(",");
                csv.append(r.checkIn().format(DATE_FORMAT)).append(",");
                csv.append(r.checkOut().format(DATE_FORMAT)).append(",");
                csv.append(r.status()).append(",");
                csv.append(formatCsvAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatCsvAmount(totalRevenue));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation r) {
        BigDecimal paid = r.amountPaid() != null ? r.amountPaid() : BigDecimal.ZERO;
        BigDecimal refunded = r.refund() != null ? r.refund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String getRoomType(String roomNumber) {
        return roomRepository.findByNumber(roomNumber)
            .map(room -> room.type().name())
            .orElse("UNKNOWN");
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}
