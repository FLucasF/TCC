package com.pousada.marealta.handlers;

import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportHandler.class);

    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(dateStr, formatter);

                StringBuilder csv = new StringBuilder();
                csv.append("room,type,reservation,check_in,check_out\n");

                int occupiedRooms = 0;
                var rooms = RoomRepository.findAll();

                for (var room : rooms) {
                    var reservations = ReservationRepository.findByRoomAndDate(room.number(), date);
                    if (!reservations.isEmpty()) {
                        occupiedRooms++;
                        for (var res : reservations) {
                            csv.append(String.format("%d,%s,%d,%s,%s\n",
                                res.getRoomNumber(),
                                room.type(),
                                res.getId(),
                                res.getCheckIn().format(formatter),
                                res.getCheckOut().format(formatter)
                            ));
                        }
                    }
                }

                csv.append(String.format("total,%d/%d", occupiedRooms, rooms.size()));

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv.toString());
            } catch (Exception e) {
                logger.error("Error generating occupancy report", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                var reservations = ReservationRepository.findByMonth(month.getYear(), month.getMonthValue());

                StringBuilder csv = new StringBuilder();
                csv.append("reservation,room,check_in,check_out,status,amount\n");

                BigDecimal total = BigDecimal.ZERO;
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (var res : reservations) {
                    BigDecimal amount = res.getAmountPaid();
                    if (res.getRefund() != null) {
                        amount = amount.subtract(res.getRefund());
                    }

                    csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                        res.getId(),
                        res.getRoomNumber(),
                        res.getCheckIn().format(dateFormatter),
                        res.getCheckOut().format(dateFormatter),
                        res.getStatus(),
                        formatBigDecimal(amount)
                    ));

                    total = total.add(amount);
                }

                csv.append(String.format("total,,,,%s", formatBigDecimal(total)));

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv.toString());
            } catch (Exception e) {
                logger.error("Error generating revenue report", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });
    }

    private static String formatBigDecimal(BigDecimal value) {
        return String.format("%.2f", value);
    }

    record ErrorResponse(String error, String message) {}
}
