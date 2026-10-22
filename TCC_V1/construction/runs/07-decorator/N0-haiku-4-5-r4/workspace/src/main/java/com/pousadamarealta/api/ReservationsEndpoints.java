package com.pousadamarealta.api;

import io.javalin.Javalin;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.MessageService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationsEndpoints {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                JsonObject body = com.google.gson.JsonParser.parseString(ctx.body()).getAsJsonObject();

                if (!hasRequiredFields(body)) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                int roomNumber = getInt(body, "roomNumber");
                String guestName = getString(body, "guestName");
                String guestEmail = getString(body, "guestEmail");
                String guestCpf = getString(body, "guestCpf");
                LocalDate checkIn = parseDate(getString(body, "checkIn"));
                LocalDate checkOut = parseDate(getString(body, "checkOut"));
                int guests = getInt(body, "guests");
                String ratePlan = getString(body, "ratePlan");

                if (checkIn == null || checkOut == null || checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid dates"));
                    return;
                }

                Room room = RoomRepository.findByNumber(roomNumber);
                if (room == null) {
                    ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                    return;
                }

                if (guests < 1 || guests > room.capacity) {
                    ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Invalid guest count"));
                    return;
                }

                if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
                    ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available"));
                    return;
                }

                int nights = PricingService.calculateNights(checkIn, checkOut);
                PricingService.PriceCalculation price = PricingService.calculatePrice(room.nightlyRate, checkIn, checkOut, ratePlan);

                Reservation reservation = new Reservation(
                        roomNumber, guestName, guestEmail, guestCpf,
                        checkIn, checkOut, guests, ratePlan,
                        nights, price.subtotal, price.discount, price.total
                );

                ReservationRepository.save(reservation);
                ctx.status(201).json(toJson(reservation));

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                ctx.json(toJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (!"PENDING".equals(reservation.status)) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm non-pending reservation"));
                    return;
                }

                reservation.status = "CONFIRMED";
                reservation.amountPaid = reservation.total;
                ReservationRepository.update(reservation);

                sendConfirmationEmail(reservation);

                ctx.json(toJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (!"PENDING".equals(reservation.status) && !"CONFIRMED".equals(reservation.status)) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current state"));
                    return;
                }

                if ("STANDARD".equals(reservation.ratePlan)) {
                    reservation.refund = reservation.amountPaid;
                } else {
                    reservation.refund = java.math.BigDecimal.ZERO;
                }

                reservation.status = "CANCELLED";
                ReservationRepository.update(reservation);

                ctx.json(toJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (!"CONFIRMED".equals(reservation.status)) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in non-confirmed reservation"));
                    return;
                }

                reservation.status = "CHECKED_IN";
                ReservationRepository.update(reservation);

                ctx.json(toJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (!"CHECKED_IN".equals(reservation.status)) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out reservation not checked in"));
                    return;
                }

                reservation.status = "CHECKED_OUT";
                ReservationRepository.update(reservation);

                ctx.json(toReceiptJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.amountPaid.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                    return;
                }

                ctx.json(toReceiptJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }

    private static boolean hasRequiredFields(JsonObject obj) {
        return obj.has("roomNumber") && obj.has("guestName") && obj.has("guestEmail") &&
               obj.has("guestCpf") && obj.has("checkIn") && obj.has("checkOut") &&
               obj.has("guests") && obj.has("ratePlan");
    }

    private static int getInt(JsonObject obj, String key) {
        return obj.get(key).getAsInt();
    }

    private static String getString(JsonObject obj, String key) {
        return obj.get(key).getAsString();
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private static void sendConfirmationEmail(Reservation reservation) throws SQLException {
        Room room = RoomRepository.findByNumber(reservation.roomNumber);
        String receipt = generateReceipt(reservation, room);
        String emailBody = "Olá, " + reservation.guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receipt + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        MessageService.sendMessage("EMAIL", reservation.guestEmail,
                "Reserva confirmada - Pousada Maré Alta", emailBody);
    }

    private static String generateReceipt(Reservation reservation, Room room) {
        String nighsStr = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        receipt.append("Quarto: ").append(reservation.roomNumber).append(" (").append(room.type).append(")\n");
        receipt.append("Entrada: ").append(formatDate(reservation.checkIn)).append(" | ");
        receipt.append("Saída: ").append(formatDate(reservation.checkOut)).append(" | ").append(nighsStr).append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.subtotal)).append("\n");
        if (reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.discount)).append("\n");
        }
        receipt.append("Total: ").append(formatMoney(reservation.total)).append("\n");
        return receipt.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatMoney(java.math.BigDecimal value) {
        return "R$ " + String.format("%.2f", value).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() >= 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private static JsonObject toJson(Reservation r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", r.id);
        obj.addProperty("roomNumber", r.roomNumber);
        obj.addProperty("guestName", r.guestName);
        obj.addProperty("guestEmail", r.guestEmail);
        obj.addProperty("guestCpf", r.guestCpf);
        obj.addProperty("checkIn", r.checkIn.toString());
        obj.addProperty("checkOut", r.checkOut.toString());
        obj.addProperty("guests", r.guests);
        obj.addProperty("ratePlan", r.ratePlan);
        obj.addProperty("nights", r.nights);
        obj.addProperty("subtotal", r.subtotal);
        obj.addProperty("discount", r.discount);
        obj.addProperty("total", r.total);
        obj.addProperty("status", r.status);
        obj.addProperty("amountPaid", r.amountPaid);
        if (r.refund != null) {
            obj.addProperty("refund", r.refund);
        } else {
            obj.add("refund", null);
        }
        return obj;
    }

    private static JsonObject toReceiptJson(Reservation r) throws SQLException {
        Room room = RoomRepository.findByNumber(r.roomNumber);
        JsonObject obj = new JsonObject();
        obj.addProperty("title", "Comprovante de reserva");
        obj.addProperty("reservationId", r.id);
        obj.addProperty("guestName", r.guestName);
        obj.addProperty("guestCpf", r.guestCpf);
        obj.addProperty("roomNumber", r.roomNumber);
        obj.addProperty("roomType", room.type);
        obj.addProperty("checkIn", r.checkIn.toString());
        obj.addProperty("checkOut", r.checkOut.toString());
        obj.addProperty("nights", r.nights);
        obj.addProperty("subtotal", r.subtotal);
        obj.addProperty("discount", r.discount);
        obj.addProperty("total", r.total);
        return obj;
    }
}
