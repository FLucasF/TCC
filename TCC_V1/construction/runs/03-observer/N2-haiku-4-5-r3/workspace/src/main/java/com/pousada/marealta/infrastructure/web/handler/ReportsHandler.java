package com.pousada.marealta.infrastructure.web.handler;

import com.pousada.marealta.domain.repository.ReservationRepository;
import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.room.Room;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportsHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportsHandler.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportsHandler(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void occupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.result("Missing required query parameter: date");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
            List<Reservation> confirmedReservations = reservationRepository.findConfirmedByDate(date);
            List<Room> allRooms = roomRepository.findAll();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            for (Room room : allRooms) {
                var reservation = confirmedReservations.stream()
                        .filter(r -> r.room().number() == room.number())
                        .findFirst();

                if (reservation.isPresent()) {
                    Reservation res = reservation.get();
                    csv.append(room.number()).append(",")
                            .append(room.type().displayName()).append(",")
                            .append(res.id()).append(",")
                            .append(res.dates().checkIn().format(DATE_FORMAT)).append(",")
                            .append(res.dates().checkOut().format(DATE_FORMAT)).append("\n");
                }
            }

            int occupied = (int) confirmedReservations.stream()
                    .map(r -> r.room().number())
                    .distinct()
                    .count();
            csv.append("total,").append(occupied).append("/").append(allRooms.size());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            logger.error("Error generating occupancy report", e);
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("Invalid date format. Use yyyy-MM-dd");
        }
    }

    public void revenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.result("Missing required query parameter: month");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, MONTH_FORMAT);
            List<Reservation> reservations = reservationRepository.findByMonth(month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            for (Reservation res : reservations) {
                csv.append(res.id()).append(",")
                        .append(res.room().number()).append(",")
                        .append(res.dates().checkIn().format(DATE_FORMAT)).append(",")
                        .append(res.dates().checkOut().format(DATE_FORMAT)).append(",")
                        .append(res.status().displayName()).append(",")
                        .append(String.format("%.2f", calculateNetAmount(res))).append("\n");
            }

            double total = reservations.stream()
                    .mapToDouble(this::calculateNetAmount)
                    .sum();
            csv.append("total,,,,").append(String.format("%.2f", total));

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            logger.error("Error generating revenue report", e);
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("Invalid month format. Use yyyy-MM");
        }
    }

    private double calculateNetAmount(Reservation res) {
        double paid = res.amountPaid().doubleValue();
        double refunded = res.refund() != null ? res.refund().doubleValue() : 0;
        return paid - refunded;
    }
}
