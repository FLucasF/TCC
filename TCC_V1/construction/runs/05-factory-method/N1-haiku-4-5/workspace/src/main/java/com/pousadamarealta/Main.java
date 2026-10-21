package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.Repository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            port = Integer.parseInt(portEnv);
        }

        Repository repository = new Repository();
        PricingService pricingService = new PricingService();
        DocumentGenerator documentGenerator = new DocumentGenerator(pricingService);
        ReservationService reservationService = new ReservationService(repository, pricingService, documentGenerator);
        ReportService reportService = new ReportService(repository);

        Javalin app = Javalin.create().start(port);

        app.get("/rooms", ctx -> {
            List<Map<String, Object>> rooms = new ArrayList<>();
            for (Room room : repository.getAllRooms()) {
                Map<String, Object> roomData = new LinkedHashMap<>();
                roomData.put("number", room.number);
                roomData.put("type", room.type);
                roomData.put("capacity", room.capacity);
                roomData.put("nightlyRate", room.nightlyRate);
                rooms.add(roomData);
            }
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                Integer roomNumber = parseInteger(body.get("roomNumber"));
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                LocalDate checkIn = parseDate((String) body.get("checkIn"));
                LocalDate checkOut = parseDate((String) body.get("checkOut"));
                Integer guests = parseInteger(body.get("guests"));
                String ratePlan = (String) body.get("ratePlan");

                validateRequired(roomNumber, "roomNumber");
                validateRequired(guestName, "guestName");
                validateRequired(guestEmail, "guestEmail");
                validateRequired(checkIn, "checkIn");
                validateRequired(checkOut, "checkOut");
                validateRequired(guests, "guests");
                validateRequired(ratePlan, "ratePlan");

                GuestIdentification identification = parseIdentification(body);
                if (identification == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Must provide either guestCpf, companyCnpj+companyName, or guestPassport+guestCountry"));
                    return;
                }

                Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail,
                                                                              identification, checkIn, checkOut, guests, ratePlan);
                ctx.status(HttpStatus.CREATED);
                ctx.json(serializeReservation(reservation));
            } catch (ReservationService.ReservationException e) {
                handleReservationException(ctx, e);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = repository.getReservation(id);
                if (reservation == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                } else {
                    ctx.json(serializeReservation(reservation));
                }
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

                Reservation reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(serializeReservation(reservation));
            } catch (ReservationService.ReservationException e) {
                handleReservationException(ctx, e);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(serializeReservation(reservation));
            } catch (ReservationService.ReservationException e) {
                handleReservationException(ctx, e);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(serializeReservation(reservation));
            } catch (ReservationService.ReservationException e) {
                handleReservationException(ctx, e);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                Room room = repository.getRoom(reservation.roomNumber);
                Document document = documentGenerator.generate(reservation, room);
                ctx.json(serializeDocument(document));
            } catch (ReservationService.ReservationException e) {
                handleReservationException(ctx, e);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = repository.getReservation(id);
                if (reservation == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                    return;
                }

                if (reservation.status != Reservation.Status.CONFIRMED &&
                    reservation.status != Reservation.Status.CHECKED_IN &&
                    reservation.status != Reservation.Status.CHECKED_OUT) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Document only available for paid reservations"));
                    return;
                }

                Room room = repository.getRoom(reservation.roomNumber);
                Document document = documentGenerator.generate(reservation, room);
                ctx.json(serializeDocument(document));
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            List<Map<String, Object>> messages = new ArrayList<>();
            for (Message message : repository.getAllMessages()) {
                Map<String, Object> messageData = new LinkedHashMap<>();
                messageData.put("id", message.id);
                messageData.put("channel", message.channel);
                messageData.put("to", message.to);
                messageData.put("subject", message.subject);
                messageData.put("body", message.body);
                messages.add(messageData);
            }
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "date parameter is required"));
                return;
            }
            try {
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                return;
            }
            try {
                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        System.out.println("Pousada Maré Alta reservations system started on port " + port);
    }

    private static Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private static void validateRequired(Object value, String fieldName) throws Exception {
        if (value == null) {
            throw new Exception("Missing required field: " + fieldName);
        }
    }

    private static GuestIdentification parseIdentification(Map<String, Object> body) {
        String guestCpf = (String) body.get("guestCpf");
        String companyCnpj = (String) body.get("companyCnpj");
        String companyName = (String) body.get("companyName");
        String guestPassport = (String) body.get("guestPassport");
        String guestCountry = (String) body.get("guestCountry");

        int count = 0;
        if (guestCpf != null) count++;
        if (companyCnpj != null && companyName != null) count++;
        if (guestPassport != null && guestCountry != null) count++;

        if (count != 1) {
            return null;
        }

        if (guestCpf != null) {
            return new GuestIdentification.CPF(guestCpf);
        } else if (companyCnpj != null && companyName != null) {
            return new GuestIdentification.CNPJ(companyCnpj, companyName);
        } else if (guestPassport != null && guestCountry != null) {
            return new GuestIdentification.Passport(guestPassport, guestCountry);
        }

        return null;
    }

    private static Map<String, Object> serializeReservation(Reservation res) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", res.id);
        data.put("roomNumber", res.roomNumber);
        data.put("guestName", res.guestName);
        data.put("guestEmail", res.guestEmail);

        if (res.identification instanceof GuestIdentification.CPF cpf) {
            data.put("guestCpf", cpf.value);
            data.put("companyCnpj", null);
            data.put("companyName", null);
            data.put("guestPassport", null);
            data.put("guestCountry", null);
        } else if (res.identification instanceof GuestIdentification.CNPJ cnpj) {
            data.put("guestCpf", null);
            data.put("companyCnpj", cnpj.value);
            data.put("companyName", cnpj.companyName);
            data.put("guestPassport", null);
            data.put("guestCountry", null);
        } else if (res.identification instanceof GuestIdentification.Passport passport) {
            data.put("guestCpf", null);
            data.put("companyCnpj", null);
            data.put("companyName", null);
            data.put("guestPassport", passport.number);
            data.put("guestCountry", passport.country);
        }

        data.put("checkIn", res.checkIn.toString());
        data.put("checkOut", res.checkOut.toString());
        data.put("guests", res.guests);
        data.put("ratePlan", res.ratePlan.name());
        data.put("nights", res.nights);
        data.put("subtotal", res.subtotal);
        data.put("discount", res.discount);
        data.put("total", res.total);
        data.put("status", res.status.name());
        data.put("amountPaid", res.amountPaid);
        data.put("refund", res.refund);

        return data;
    }

    private static Map<String, Object> serializeDocument(Document doc) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", doc.title);
        data.put("reservationId", doc.reservationId);
        data.put("guestName", doc.guestName);
        data.put("guestCpf", doc.guestCpf);
        data.put("companyCnpj", doc.companyCnpj);
        data.put("companyName", doc.companyName);
        data.put("guestPassport", doc.guestPassport);
        data.put("guestCountry", doc.guestCountry);
        data.put("roomNumber", doc.roomNumber);
        data.put("roomType", doc.roomType);
        data.put("checkIn", doc.checkIn.toString());
        data.put("checkOut", doc.checkOut.toString());
        data.put("nights", doc.nights);
        data.put("subtotal", doc.subtotal);
        data.put("discount", doc.discount);
        data.put("total", doc.total);
        data.put("text", doc.text);

        return data;
    }

    private static void handleReservationException(io.javalin.http.Context ctx, ReservationService.ReservationException e) {
        int status = switch (e.code) {
            case "ROOM_NOT_FOUND" -> 404;
            case "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE" -> 409;
            case "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
        ctx.status(status);
        ctx.json(Map.of("error", e.code, "message", e.getMessage()));
    }
}
