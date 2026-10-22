package com.pousadamareal.handlers;

import com.pousadamareal.dao.ReservationDAO;
import com.pousadamareal.dao.RoomDAO;
import com.pousadamareal.model.Reservation;
import com.pousadamareal.model.Room;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;

public class ReportHandler {
    public static void getOccupancyReport(Context ctx) {
        try {
            String date = ctx.queryParam("date");
            if (date == null || date.isEmpty()) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' obrigatório"));
                ctx.status(400);
                return;
            }

            try {
                LocalDate.parse(date);
            } catch (Exception e) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Data inválida (formato: yyyy-MM-dd)"));
                ctx.status(400);
                return;
            }

            List<Reservation> reservations = ReservationDAO.getReservationsByDateAndStatus(date, "CONFIRMED");
            List<Reservation> checkedInReservations = ReservationDAO.getReservationsByDateAndStatus(date, "CHECKED_IN");
            List<Reservation> checkedOutReservations = ReservationDAO.getReservationsByDateAndStatus(date, "CHECKED_OUT");

            reservations.addAll(checkedInReservations);
            reservations.addAll(checkedOutReservations);

            List<Room> rooms = RoomDAO.getAllRooms();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            int occupiedCount = 0;
            for (Room room : rooms) {
                boolean occupied = false;
                for (Reservation res : reservations) {
                    if (res.roomNumber == room.number()) {
                        csv.append(room.number()).append(",")
                           .append(room.type()).append(",")
                           .append(res.id).append(",")
                           .append(res.checkIn).append(",")
                           .append(res.checkOut).append("\n");
                        occupied = true;
                        occupiedCount++;
                        break;
                    }
                }
            }

            csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public static void getRevenueReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' obrigatório"));
                ctx.status(400);
                return;
            }

            try {
                String[] parts = month.split("-");
                if (parts.length != 2) {
                    throw new Exception("Formato inválido");
                }
                Integer.parseInt(parts[0]);
                Integer.parseInt(parts[1]);
            } catch (Exception e) {
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Mês inválido (formato: yyyy-MM)"));
                ctx.status(400);
                return;
            }

            List<Reservation> reservations = ReservationDAO.getReservationsByMonth(month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            double totalAmount = 0;
            for (Reservation res : reservations) {
                double amount = res.amountPaid;
                if (res.refund != null) {
                    amount -= res.refund;
                }

                csv.append(res.id).append(",")
                   .append(res.roomNumber).append(",")
                   .append(res.checkIn).append(",")
                   .append(res.checkOut).append(",")
                   .append(res.status).append(",")
                   .append(String.format("%.2f", amount)).append("\n");

                totalAmount += amount;
            }

            csv.append("total,").append(String.format("%.2f", totalAmount));

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public record ErrorResponse(String error, String message) {}
}
