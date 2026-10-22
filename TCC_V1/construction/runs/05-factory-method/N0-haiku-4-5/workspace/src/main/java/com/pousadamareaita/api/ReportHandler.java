package com.pousadamareaita.api;

import io.javalin.http.Context;
import com.pousadamareaita.database.Database;
import com.pousadamareaita.service.ReservationService;
import com.pousadamareaita.service.RoomService;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.exception.ApiException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

public class ReportHandler {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportHandler(Database db) {
        this.reservationService = new ReservationService(db);
        this.roomService = new RoomService(db);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                throw new ApiException("INVALID_REQUEST", "date parameter is required", 400);
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid date format, use YYYY-MM-DD", 400);
            }

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            List<Room> rooms = roomService.getAllRooms();
            int occupiedRooms = 0;

            for (Room room : rooms) {
                List<Reservation> reservations = reservationService.getReservationsByRoomAndDate(room.getNumber(), date);

                boolean roomOccupied = false;
                for (Reservation res : reservations) {
                    if (res.getStatus().equals("CONFIRMED") || res.getStatus().equals("CHECKED_IN") || res.getStatus().equals("CHECKED_OUT")) {
                        csv.append(room.getNumber()).append(",");
                        csv.append(room.getType()).append(",");
                        csv.append(res.getId()).append(",");
                        csv.append(res.getCheckIn()).append(",");
                        csv.append(res.getCheckOut()).append("\n");
                        if (!roomOccupied) {
                            occupiedRooms++;
                            roomOccupied = true;
                        }
                    }
                }
            }

            csv.append("total,").append(occupiedRooms).append("/").append(rooms.size());

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new ApiException("INVALID_REQUEST", "month parameter is required", 400);
            }

            YearMonth month;
            try {
                month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid month format, use YYYY-MM", 400);
            }

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            List<Reservation> reservations = reservationService.getReservationsByMonthWithPayment(month);
            BigDecimal total = BigDecimal.ZERO;

            for (Reservation res : reservations) {
                BigDecimal money = res.getAmountPaid();
                if (res.getRefund() != null) {
                    money = money.subtract(res.getRefund());
                }

                csv.append(res.getId()).append(",");
                csv.append(res.getRoomNumber()).append(",");
                csv.append(res.getCheckIn()).append(",");
                csv.append(res.getCheckOut()).append(",");
                csv.append(res.getStatus()).append(",");
                csv.append(formatAmount(money)).append("\n");

                total = total.add(money);
            }

            csv.append("total,,,,").append(formatAmount(total));

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%.2f", amount);
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
