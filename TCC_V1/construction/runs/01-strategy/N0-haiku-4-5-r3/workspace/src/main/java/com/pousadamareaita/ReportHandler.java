package com.pousadamareaita;

import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.repository.ReservationRepository;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportHandler.class);
    private static final ReservationRepository reservationRepo = new ReservationRepository();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter csvDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void occupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400);
                ctx.json(java.util.Map.of("error", "INVALID_REQUEST", "message", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            List<Reservation> reservations = reservationRepo.findByDate(date);

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            for (Reservation reservation : reservations) {
                String type = switch (reservation.getRoomNumber()) {
                    case 101, 102 -> "STANDARD";
                    case 201, 202 -> "DELUXE";
                    case 301 -> "SUITE";
                    default -> "UNKNOWN";
                };

                String checkInStr = reservation.getCheckIn().format(csvDateFormatter);
                String checkOutStr = reservation.getCheckOut().format(csvDateFormatter);

                csv.append(reservation.getRoomNumber()).append(",")
                   .append(type).append(",")
                   .append(reservation.getId()).append(",")
                   .append(checkInStr).append(",")
                   .append(checkOutStr).append("\n");
            }

            csv.append("total,").append(reservations.size()).append("/5\n");

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            logger.error("Error generating occupancy report", e);
            ctx.status(400);
            ctx.json(java.util.Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        }
    }

    public static void revenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400);
                ctx.json(java.util.Map.of("error", "INVALID_REQUEST", "message", "Missing month parameter"));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400);
                ctx.json(java.util.Map.of("error", "INVALID_REQUEST", "message", "Invalid month format (use YYYY-MM)"));
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            List<Reservation> reservations = reservationRepo.findByMonth(month, year);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;

            for (Reservation reservation : reservations) {
                String checkInStr = reservation.getCheckIn().format(csvDateFormatter);
                String checkOutStr = reservation.getCheckOut().format(csvDateFormatter);

                BigDecimal amount = reservation.getAmountPaid();
                if (reservation.getRefund() != null) {
                    amount = amount.subtract(reservation.getRefund());
                }

                csv.append(reservation.getId()).append(",")
                   .append(reservation.getRoomNumber()).append(",")
                   .append(checkInStr).append(",")
                   .append(checkOutStr).append(",")
                   .append(reservation.getStatus()).append(",")
                   .append(String.format("%.2f", amount).replace(",", ".")).append("\n");

                total = total.add(amount);
            }

            csv.append("total,,,,").append(String.format("%.2f", total).replace(",", ".")).append("\n");

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            logger.error("Error generating revenue report", e);
            ctx.status(400);
            ctx.json(java.util.Map.of("error", "INVALID_REQUEST", "message", "Invalid month format (use YYYY-MM)"));
        }
    }
}
