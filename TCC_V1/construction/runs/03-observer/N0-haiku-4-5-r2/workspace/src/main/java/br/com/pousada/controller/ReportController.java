package br.com.pousada.controller;

import br.com.pousada.model.Reservation;
import br.com.pousada.repository.ReservationRepository;
import br.com.pousada.repository.RoomRepository;
import br.com.pousada.util.DateUtil;
import br.com.pousada.util.MoneyUtil;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private static final ReservationRepository reservationRepository = new ReservationRepository();
    private static final RoomRepository roomRepository = new RoomRepository();

    public static void routes(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' é obrigatório"));
                    return;
                }

                LocalDate date = DateUtil.parseIsoDate(dateStr);
                List<Reservation> occupancy = reservationRepository.findOccupancyByDate(date);

                StringBuilder csv = new StringBuilder();
                csv.append("room,type,reservation,check_in,check_out\n");

                for (Reservation r : occupancy) {
                    csv.append(String.format("%d,%s,%d,%s,%s\n",
                        r.roomNumber,
                        getQuartoType(r.roomNumber),
                        r.id,
                        DateUtil.formatIsoDate(r.checkIn),
                        DateUtil.formatIsoDate(r.checkOut)));
                }

                csv.append(String.format("total,%d/5", occupancy.size()));

                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(csv.toString());
            } catch (Exception e) {
                logger.error("Erro ao gerar relatório de ocupação", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de data inválido"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório"));
                    return;
                }

                YearMonth month = DateUtil.parseYearMonth(monthStr);
                List<Reservation> revenue = reservationRepository.findByMonthWithPayment(month);

                StringBuilder csv = new StringBuilder();
                csv.append("reservation,room,check_in,check_out,status,amount\n");

                double total = 0;
                for (Reservation r : revenue) {
                    double amount = r.amountPaid - (r.refund != null ? r.refund : 0);
                    csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                        r.id,
                        r.roomNumber,
                        DateUtil.formatIsoDate(r.checkIn),
                        DateUtil.formatIsoDate(r.checkOut),
                        r.status.toString(),
                        amount));
                    total += amount;
                }

                csv.append(String.format("total,%.2f", total));

                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(csv.toString());
            } catch (Exception e) {
                logger.error("Erro ao gerar relatório financeiro", e);
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de mês inválido (use YYYY-MM)"));
            }
        });
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    private static String getQuartoType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
