package br.pousada.marealta.api;

import io.javalin.Javalin;
import br.pousada.marealta.database.Database;
import br.pousada.marealta.database.ReservationRepository;
import br.pousada.marealta.database.RoomRepository;
import br.pousada.marealta.model.Reservation;
import br.pousada.marealta.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportHandler.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
                String csv = generateOccupancyReport(date);
                ctx.contentType("text/csv;charset=utf-8").result(csv);
            } catch (Exception e) {
                logger.error("Error generating occupancy report", e);
                ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to generate report"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(ErrorResponse.error("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                String csv = generateRevenueReport(monthStr);
                ctx.contentType("text/csv;charset=utf-8").result(csv);
            } catch (Exception e) {
                logger.error("Error generating revenue report", e);
                ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to generate report"));
            }
        });
    }

    private static String generateOccupancyReport(LocalDate date) throws Exception {
        try (Connection conn = Database.getConnection()) {
            List<Reservation> reservations = ReservationRepository.findByDate(conn, date);
            List<Room> rooms = RoomRepository.findAll();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            Map<Integer, Reservation> resMap = reservations.stream()
                .collect(Collectors.toMap(Reservation::getRoomNumber, r -> r));

            for (Room room : rooms) {
                if (resMap.containsKey(room.getNumber())) {
                    Reservation res = resMap.get(room.getNumber());
                    csv.append(String.format("%d,%s,%d,%s,%s\n",
                        room.getNumber(),
                        room.getType(),
                        res.getId(),
                        res.getCheckIn().format(DATE_FORMAT),
                        res.getCheckOut().format(DATE_FORMAT)));
                }
            }

            csv.append(String.format("total,%d/%d\n", reservations.size(), rooms.size()));

            return csv.toString();
        }
    }

    private static String generateRevenueReport(String month) throws Exception {
        try (Connection conn = Database.getConnection()) {
            List<Reservation> reservations = ReservationRepository.findByMonth(conn, month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            double total = 0;
            for (Reservation res : reservations) {
                double amount = res.getAmountPaid();
                if (res.getRefund() != null) {
                    amount -= res.getRefund();
                }

                csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                    res.getId(),
                    res.getRoomNumber(),
                    res.getCheckIn().format(DATE_FORMAT),
                    res.getCheckOut().format(DATE_FORMAT),
                    res.getStatus(),
                    amount));

                total += amount;
            }

            csv.append(String.format("total,%.2f\n", total));

            return csv.toString();
        }
    }
}
