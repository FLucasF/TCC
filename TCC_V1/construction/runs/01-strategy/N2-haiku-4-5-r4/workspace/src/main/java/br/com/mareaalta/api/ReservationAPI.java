package br.com.mareaalta.api;

import br.com.mareaalta.domain.*;
import br.com.mareaalta.service.ReservationService;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;
import com.google.gson.*;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.*;

public class ReservationAPI {
    private final Javalin app;
    private final ReservationService service;
    private final Gson gson;

    public ReservationAPI(Javalin app, ReservationService service) {
        this.app = app;
        this.service = service;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                    public LocalDate deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                                 JsonDeserializationContext context) throws JsonSyntaxException {
                        return DateUtils.parse(json.getAsString());
                    }
                })
                .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                    public JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfT,
                                                JsonSerializationContext context) {
                        return new JsonPrimitive(DateUtils.format(src));
                    }
                })
                .setPrettyPrinting()
                .create();

        setupRoutes();
    }

    private void setupRoutes() {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getRooms(io.javalin.http.Context ctx) {
        try {
            List<Room> rooms = service.getAllRooms();
            List<Map<String, Object>> roomList = new ArrayList<>();

            for (Room room : rooms) {
                Map<String, Object> roomMap = new LinkedHashMap<>();
                roomMap.put("number", room.getNumber());
                roomMap.put("type", room.getType().getDisplayName());
                roomMap.put("capacity", room.getCapacity());
                roomMap.put("nightlyRate", room.getNightlyRate());
                roomList.add(roomMap);
            }

            ctx.json(roomList);
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            Integer roomNumber = getInt(body, "roomNumber");
            String guestName = getString(body, "guestName");
            String guestEmail = getString(body, "guestEmail");
            String guestCpf = getString(body, "guestCpf");
            String checkInStr = getString(body, "checkIn");
            String checkOutStr = getString(body, "checkOut");
            Integer guests = getInt(body, "guests");
            String ratePlanStr = getString(body, "ratePlan");
            String companyCode = getString(body, "companyCode");

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                    checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Campo obrigatório faltando"));
                return;
            }

            LocalDate checkIn = DateUtils.parse(checkInStr);
            LocalDate checkOut = DateUtils.parse(checkOutStr);
            RatePlanType ratePlan = RatePlanType.fromString(ratePlanStr);

            if (checkIn == null || checkOut == null || ratePlan == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Dados inválidos"));
                return;
            }

            var result = service.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan, companyCode);

            if (!result.isSuccess()) {
                int statusCode = switch (result.getErrorCode()) {
                    case "ROOM_NOT_FOUND" -> 404;
                    case "ROOM_UNAVAILABLE" -> 409;
                    case "CAPACITY_EXCEEDED" -> 422;
                    case "INVALID_REQUEST" -> 400;
                    default -> 500;
                };
                ctx.status(statusCode).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.status(201).json(reservationToJson(result.getData()));
        } catch (JsonSyntaxException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "JSON inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var result = service.getReservation(id);

            if (!result.isSuccess()) {
                ctx.status(404).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.json(reservationToJson(result.getData()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            Double amount = getDouble(body, "amount");
            if (amount == null || amount <= 0) {
                ctx.status(400).json(error("INVALID_REQUEST", "Valor de pagamento inválido"));
                return;
            }

            var result = service.confirmReservation(id, amount);

            if (!result.isSuccess()) {
                int statusCode = switch (result.getErrorCode()) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 500;
                };
                ctx.status(statusCode).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.json(reservationToJson(result.getData()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        } catch (JsonSyntaxException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "JSON inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void checkIn(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var result = service.checkIn(id);

            if (!result.isSuccess()) {
                int statusCode = switch (result.getErrorCode()) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 500;
                };
                ctx.status(statusCode).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.json(reservationToJson(result.getData()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void checkOut(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var result = service.checkOut(id);

            if (!result.isSuccess()) {
                int statusCode = switch (result.getErrorCode()) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 500;
                };
                ctx.status(statusCode).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.json(reservationToJson(result.getData()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var result = service.cancelReservation(id);

            if (!result.isSuccess()) {
                int statusCode = switch (result.getErrorCode()) {
                    case "RESERVATION_NOT_FOUND" -> 404;
                    case "OPERATION_NOT_ALLOWED" -> 409;
                    default -> 500;
                };
                ctx.status(statusCode).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            ctx.json(reservationToJson(result.getData()));
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var result = service.getReservation(id);

            if (!result.isSuccess()) {
                ctx.status(404).json(error(result.getErrorCode(), result.getErrorMessage()));
                return;
            }

            Reservation res = result.getData();
            if (res.getStatus() != ReservationStatus.CONFIRMED &&
                    res.getStatus() != ReservationStatus.CHECKED_IN &&
                    res.getStatus() != ReservationStatus.CHECKED_OUT) {
                ctx.status(400).json(error("OPERATION_NOT_ALLOWED", "Documento não disponível"));
                return;
            }

            try {
                Room room = service.getAllRooms().stream()
                        .filter(r -> r.getNumber() == res.getRoomNumber())
                        .findFirst()
                        .orElse(null);

                if (room == null) {
                    ctx.status(400).json(error("ROOM_NOT_FOUND", "Quarto não encontrado"));
                    return;
                }

                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("title", "Comprovante de reserva");
                doc.put("reservationId", res.getId());
                doc.put("guestName", res.getGuestName());
                doc.put("guestCpf", res.getGuestCpf());
                doc.put("roomNumber", res.getRoomNumber());
                doc.put("roomType", room.getType().getDisplayName());
                doc.put("checkIn", DateUtils.format(res.getCheckIn()));
                doc.put("checkOut", DateUtils.format(res.getCheckOut()));
                doc.put("nights", res.getNights());
                doc.put("subtotal", res.getSubtotal());
                doc.put("discount", res.getDiscount());
                doc.put("total", res.getTotal());

                ctx.json(doc);
            } catch (Exception e) {
                ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
            }
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("INVALID_REQUEST", "ID inválido"));
        }
    }

    private void getMessages(io.javalin.http.Context ctx) {
        try {
            List<Message> messages = service.getAllMessages();
            List<Map<String, Object>> messageList = new ArrayList<>();

            for (Message msg : messages) {
                Map<String, Object> msgMap = new LinkedHashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("channel", msg.getChannel());
                msgMap.put("to", msg.getTo());
                msgMap.put("subject", msg.getSubject());
                msgMap.put("body", msg.getBody());
                messageList.add(msgMap);
            }

            ctx.json(messageList);
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Parâmetro date é obrigatório"));
                return;
            }

            LocalDate date = DateUtils.parse(dateStr);
            if (date == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Data inválida"));
                return;
            }

            String report = service.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Parâmetro month é obrigatório"));
                return;
            }

            String report = service.getRevenueReport(monthStr);
            if (report == null) {
                ctx.status(400).json(error("INVALID_REQUEST", "Mês inválido"));
                return;
            }

            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(500).json(error("DATABASE_ERROR", e.getMessage()));
        }
    }

    private Map<String, Object> reservationToJson(Reservation res) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", res.getId());
        map.put("roomNumber", res.getRoomNumber());
        map.put("guestName", res.getGuestName());
        map.put("guestEmail", res.getGuestEmail());
        map.put("guestCpf", res.getGuestCpf());
        map.put("checkIn", DateUtils.format(res.getCheckIn()));
        map.put("checkOut", DateUtils.format(res.getCheckOut()));
        map.put("guests", res.getGuests());
        map.put("ratePlan", res.getRatePlan().toString());
        map.put("nights", res.getNights());
        map.put("subtotal", res.getSubtotal());
        map.put("discount", res.getDiscount());
        map.put("total", res.getTotal());
        map.put("status", res.getStatus().toString());
        map.put("amountPaid", res.getAmountPaid());
        map.put("refund", res.getRefund());
        return map;
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", code);
        map.put("message", message);
        return map;
    }

    private Integer getInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDouble(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
