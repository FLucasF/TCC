package com.pousadamareaalta.handlers;

import com.pousadamareaalta.repositories.ReservationRepository;
import com.pousadamareaalta.repositories.RoomRepository;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;

public class ReportHandler {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    public void occupancy(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "date parameter required";
                });
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            var rooms = roomRepository.findAll();
            int occupiedCount = 0;

            for (var room : rooms) {
                var reservations = reservationRepository.findByRoomAndDate(room.number, date);
                if (!reservations.isEmpty()) {
                    for (var res : reservations) {
                        if ("CONFIRMED".equals(res.status) || "CHECKED_IN".equals(res.status) || "CHECKED_OUT".equals(res.status)) {
                            csv.append(room.number).append(",")
                                    .append(room.type).append(",")
                                    .append(res.id).append(",")
                                    .append(res.checkIn).append(",")
                                    .append(res.checkOut).append("\n");
                            occupiedCount++;
                        }
                    }
                }
            }

            csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

            ctx.contentType("text/csv;charset=utf-8").result(csv.toString());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new Object() {
                public final String error = "INVALID_REQUEST";
                public final String message = "Invalid date format";
            });
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "month parameter required";
                });
                return;
            }

            var parts = monthParam.split("-");
            if (parts.length != 2) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "Invalid month format";
                });
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            var reservations = reservationRepository.findByMonthWithMoney(year, month);

            double totalAmount = 0;

            for (var res : reservations) {
                double amountForReport = res.amountPaid - (res.refund != null ? res.refund : 0);

                csv.append(res.id).append(",")
                        .append(res.roomNumber).append(",")
                        .append(res.checkIn).append(",")
                        .append(res.checkOut).append(",")
                        .append(res.status).append(",")
                        .append(String.format("%.2f", amountForReport)).append("\n");

                totalAmount += amountForReport;
            }

            csv.append("total,,,,").append(String.format("%.2f", totalAmount));

            ctx.contentType("text/csv;charset=utf-8").result(csv.toString());
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Object() {
                public final String error = "INVALID_REQUEST";
                public final String message = "Invalid month format";
            });
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }
}
