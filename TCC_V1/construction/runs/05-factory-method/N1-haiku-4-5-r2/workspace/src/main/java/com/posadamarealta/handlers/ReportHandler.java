package com.posadamarealta.handlers;

import com.posadamarealta.model.Reservation;
import com.posadamarealta.repository.ReservationRepository;
import com.posadamarealta.repository.RoomRepository;
import com.posadamarealta.service.UtilService;
import io.javalin.http.Context;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportHandler {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReportHandler(DataSource ds) {
        this.reservationRepo = new ReservationRepository(ds);
        this.roomRepo = new RoomRepository(ds);
    }

    public void occupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new java.util.HashMap<String, String>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Missing date parameter");
                }});
                return;
            }

            LocalDate date = UtilService.parseDate(dateStr);

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            List<String> rows = new ArrayList<>();
            int occupiedCount = 0;

            for (var room : roomRepo.findAll()) {
                List<Reservation> reservations = reservationRepo.findByRoomAndDate(room.number, date);
                if (!reservations.isEmpty()) {
                    Reservation res = reservations.get(0);
                    rows.add(room.number + "," + room.type + "," + res.id + "," +
                            UtilService.formatDateISO(res.checkIn) + "," +
                            UtilService.formatDateISO(res.checkOut));
                    occupiedCount++;
                }
            }

            rows.forEach(row -> csv.append(row).append("\n"));
            csv.append("total,").append(occupiedCount).append("/5\n");

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(400).json(new java.util.HashMap<String, String>() {{
                put("error", "INVALID_REQUEST");
                put("message", "Invalid date format");
            }});
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new java.util.HashMap<String, String>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Missing month parameter");
                }});
                return;
            }

            YearMonth yearMonth = UtilService.parseYearMonth(monthStr);
            List<Reservation> reservations = reservationRepo.findByMonthAndStarting(yearMonth);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;
            for (Reservation res : reservations) {
                BigDecimal amount = res.amountPaid;
                if (res.refund != null) {
                    amount = amount.subtract(res.refund);
                }

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    csv.append(res.id).append(",")
                       .append(res.roomNumber).append(",")
                       .append(UtilService.formatDateISO(res.checkIn)).append(",")
                       .append(UtilService.formatDateISO(res.checkOut)).append(",")
                       .append(res.status).append(",")
                       .append(UtilService.formatCSVMoney(amount)).append("\n");
                    total = total.add(amount);
                }
            }

            csv.append("total,,,,").append(UtilService.formatCSVMoney(total)).append("\n");

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(400).json(new java.util.HashMap<String, String>() {{
                put("error", "INVALID_REQUEST");
                put("message", "Invalid month format");
            }});
        }
    }
}
