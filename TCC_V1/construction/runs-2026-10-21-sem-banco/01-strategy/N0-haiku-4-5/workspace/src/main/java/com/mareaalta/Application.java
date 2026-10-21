package com.mareaalta;

import io.javalin.Javalin;
import com.mareaalta.model.*;
import com.mareaalta.service.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Application {
    private static final ReservationService reservationService = new ReservationService();
    private static final MessageService messageService = new MessageService();
    private static final ReportService reportService = new ReportService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Javalin app = Javalin.create()
            .start(port);

        app.get("/rooms", ctx -> {
            ctx.json(reservationService.getAllRooms());
        });

        app.post("/reservations", ctx -> {
            try {
                var body = ctx.bodyAsClass(ReservationRequest.class);

                String validationError = reservationService.validateCreateReservation(
                    body.roomNumber,
                    body.checkIn,
                    body.checkOut,
                    body.guests,
                    body.ratePlan,
                    body.companyCode
                );

                if (validationError != null) {
                    ctx.status(getStatusCode(validationError));
                    ctx.json(new ErrorResponse(validationError, getMessage(validationError)));
                    return;
                }

                Reservation reservation = reservationService.createReservation(
                    body.roomNumber,
                    body.guestName,
                    body.guestEmail,
                    body.guestCpf,
                    body.checkIn,
                    body.checkOut,
                    body.guests,
                    body.ratePlan,
                    body.companyCode
                );

                ctx.status(201);
                ctx.json(reservation);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                String error = reservationService.confirmReservation(id);

                if (error != null) {
                    ctx.status(getStatusCode(error));
                    ctx.json(new ErrorResponse(error, getMessage(error)));
                    return;
                }

                Reservation reservation = reservationService.getReservation(id);
                sendConfirmationEmail(reservation);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                String error = reservationService.cancelReservation(id);

                if (error != null) {
                    ctx.status(getStatusCode(error));
                    ctx.json(new ErrorResponse(error, getMessage(error)));
                    return;
                }

                Reservation reservation = reservationService.getReservation(id);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                String error = reservationService.checkIn(id);

                if (error != null) {
                    ctx.status(getStatusCode(error));
                    ctx.json(new ErrorResponse(error, getMessage(error)));
                    return;
                }

                Reservation reservation = reservationService.getReservation(id);
                ctx.json(reservation);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                String error = reservationService.checkOut(id);

                if (error != null) {
                    ctx.status(getStatusCode(error));
                    ctx.json(new ErrorResponse(error, getMessage(error)));
                    return;
                }

                Reservation reservation = reservationService.getReservation(id);
                Receipt receipt = generateReceipt(reservation);
                ctx.json(receipt);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.amountPaid == 0) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available"));
                    return;
                }

                Receipt receipt = generateReceipt(reservation);
                ctx.json(receipt);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/messages", ctx -> {
            ctx.json(messageService.getAllMessages());
        });

        app.get("/reports/occupancy", ctx -> {
            String date = ctx.queryParam("date");
            if (date == null || date.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter required"));
                return;
            }

            String report = reportService.generateOccupancyReport(date, reservationService.getAllReservations());
            if (report == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });

        app.get("/reports/revenue", ctx -> {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
                return;
            }

            String report = reportService.generateRevenueReport(month, reservationService.getAllReservations());
            if (report == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });
    }

    private static int getStatusCode(String error) {
        return switch (error) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
    }

    private static String getMessage(String error) {
        return switch (error) {
            case "INVALID_REQUEST" -> "Invalid request";
            case "ROOM_NOT_FOUND" -> "Room not found";
            case "RESERVATION_NOT_FOUND" -> "Reservation not found";
            case "ROOM_UNAVAILABLE" -> "Room is not available for the selected dates";
            case "OPERATION_NOT_ALLOWED" -> "Operation not allowed for this reservation status";
            case "CAPACITY_EXCEEDED" -> "Number of guests exceeds room capacity";
            default -> "Unknown error";
        };
    }

    private static void sendConfirmationEmail(Reservation reservation) {
        Room room = reservationService.getRoomByNumber(reservation.roomNumber);
        Receipt receipt = generateReceipt(reservation);
        String receiptText = formatReceipt(receipt);

        String emailBody = "Olá, " + reservation.guestName + "!\n\n"
            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
            + receiptText
            + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n"
            + "Até breve!";

        messageService.sendEmail(
            reservation.guestEmail,
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
    }

    private static Receipt generateReceipt(Reservation reservation) {
        Room room = reservationService.getRoomByNumber(reservation.roomNumber);
        return new Receipt(
            reservation.id,
            reservation.guestName,
            reservation.guestCpf,
            reservation.roomNumber,
            room.type(),
            reservation.checkIn,
            reservation.checkOut,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total
        );
    }

    private static String formatReceipt(Receipt receipt) {
        String nights = receipt.nights == 1 ? "1 diária" : receipt.nights + " diárias";
        String subtotalStr = String.format("R$ %.2f", receipt.subtotal).replace(".", ",");
        String discountStr = String.format("R$ %.2f", receipt.discount).replace(".", ",");
        String totalStr = String.format("R$ %.2f", receipt.total).replace(".", ",");

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.reservationId).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName).append(" (CPF ").append(receipt.guestCpf).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber).append(" (").append(receipt.roomType).append(")\n");
        sb.append("Entrada: ").append(formatDateForReceipt(receipt.checkIn)).append(" | ");
        sb.append("Saída: ").append(formatDateForReceipt(receipt.checkOut)).append(" | ");
        sb.append(nights).append("\n");
        sb.append("Diárias: ").append(subtotalStr).append("\n");

        if (receipt.discount > 0) {
            sb.append("Desconto: ").append(discountStr).append("\n");
        }

        sb.append("Total: ").append(totalStr);

        return sb.toString();
    }

    private static String formatDateForReceipt(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static class ReservationRequest {
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public String checkIn;
        public String checkOut;
        public int guests;
        public String ratePlan;
        public String companyCode;
    }
}
