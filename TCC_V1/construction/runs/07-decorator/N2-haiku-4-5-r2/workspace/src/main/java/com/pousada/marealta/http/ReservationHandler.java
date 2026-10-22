package com.pousada.marealta.http;

import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.Receipt;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public ReservationHandler(ReservationService reservationService, MessageService messageService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            JsonObject body = ctx.bodyAsClass(JsonObject.class);
            validateRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf", "checkIn", "checkOut", "guests", "ratePlan");

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), dateFormatter);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), dateFormatter);
            int guests = body.get("guests").getAsInt();
            Reservation.RatePlan ratePlan = Reservation.RatePlan.valueOf(body.get("ratePlan").getAsString());

            Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);

            ctx.status(HttpStatus.CREATED);
            ctx.json(toJson(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.findById(id);

            if (reservation.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(createErrorJson("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }

            ctx.json(toJson(reservation.get()));
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject body = ctx.bodyAsClass(JsonObject.class);
            validateRequiredFields(body, "amount");

            BigDecimal amount = new BigDecimal(body.get("amount").getAsString());
            Reservation reservation = reservationService.confirm(id, amount);

            Message message = new Message(Message.Channel.EMAIL, reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", buildConfirmationEmail(reservation));
            messageService.sendMessage(message);

            ctx.json(toJson(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancel(id);
            ctx.json(toJson(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toJson(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(buildReceipt(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    public void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.findById(id);

            if (reservation.isEmpty() || reservation.get().getStatus() != Reservation.Status.CONFIRMED &&
                reservation.get().getStatus() != Reservation.Status.CHECKED_IN &&
                reservation.get().getStatus() != Reservation.Status.CHECKED_OUT) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.json(buildReceipt(reservation.get()));
        } catch (Exception e) {
            handleError(ctx, "INVALID_REQUEST", "Erro ao processar requisição");
        }
    }

    private JsonObject toJson(Reservation reservation) {
        JsonObject json = new JsonObject();
        json.addProperty("id", reservation.getId());
        json.addProperty("roomNumber", reservation.getRoomNumber());
        json.addProperty("guestName", reservation.getGuestName());
        json.addProperty("guestEmail", reservation.getGuestEmail());
        json.addProperty("guestCpf", reservation.getGuestCpf());
        json.addProperty("checkIn", reservation.getCheckIn().toString());
        json.addProperty("checkOut", reservation.getCheckOut().toString());
        json.addProperty("guests", reservation.getGuests());
        json.addProperty("ratePlan", reservation.getRatePlan().name());
        json.addProperty("nights", reservation.getNights());
        json.addProperty("subtotal", String.format("%.2f", reservation.getSubtotal()));
        json.addProperty("discount", String.format("%.2f", reservation.getDiscount()));
        json.addProperty("total", String.format("%.2f", reservation.getTotal()));
        json.addProperty("status", reservation.getStatus().name());
        json.addProperty("amountPaid", String.format("%.2f", reservation.getAmountPaid()));
        if (reservation.getRefund() != null) {
            json.addProperty("refund", String.format("%.2f", reservation.getRefund()));
        } else {
            json.add("refund", null);
        }
        return json;
    }

    private JsonObject buildReceipt(Reservation reservation) {
        String roomType = roomRepository.getRoomType(reservation.getRoomNumber());
        Receipt receipt = new Receipt(reservation.getId(), reservation.getGuestName(), reservation.getGuestCpf(),
            reservation.getRoomNumber(), roomType, reservation.getCheckIn(),
            reservation.getCheckOut(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal());

        JsonObject json = new JsonObject();
        json.addProperty("title", "Comprovante de reserva");
        json.addProperty("reservationId", receipt.getReservationId());
        json.addProperty("guestName", receipt.getGuestName());
        json.addProperty("guestCpf", receipt.getGuestCpf());
        json.addProperty("roomNumber", receipt.getRoomNumber());
        json.addProperty("roomType", receipt.getRoomType());
        json.addProperty("checkIn", receipt.getCheckIn().toString());
        json.addProperty("checkOut", receipt.getCheckOut().toString());
        json.addProperty("nights", receipt.getNights());
        json.addProperty("subtotal", String.format("%.2f", receipt.getSubtotal()));
        json.addProperty("discount", String.format("%.2f", receipt.getDiscount()));
        json.addProperty("total", String.format("%.2f", receipt.getTotal()));
        return json;
    }

    private String buildConfirmationEmail(Reservation reservation) {
        String roomType = roomRepository.getRoomType(reservation.getRoomNumber());
        Receipt receipt = new Receipt(reservation.getId(), reservation.getGuestName(), reservation.getGuestCpf(),
            reservation.getRoomNumber(), roomType, reservation.getCheckIn(),
            reservation.getCheckOut(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal());

        return "Olá, " + reservation.getGuestName() + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            receipt.format() + "\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";
    }

    private void validateRequiredFields(JsonObject body, String... fields) {
        for (String field : fields) {
            if (!body.has(field) || body.get(field).isJsonNull()) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        }
    }

    private void handleError(Context ctx, String message) {
        if (message.equals("Quarto não encontrado")) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(createErrorJson("ROOM_NOT_FOUND", message));
        } else if (message.equals("Reserva não encontrada")) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(createErrorJson("RESERVATION_NOT_FOUND", message));
        } else if (message.equals("Quarto não disponível")) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(createErrorJson("ROOM_UNAVAILABLE", message));
        } else if (message.equals("Operação não permitida")) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(createErrorJson("OPERATION_NOT_ALLOWED", message));
        } else if (message.equals("Número de hóspedes inválido")) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(createErrorJson("CAPACITY_EXCEEDED", message));
        } else if (message.equals("Datas inválidas")) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorJson("INVALID_REQUEST", message));
        } else if (message.equals("INVALID_REQUEST")) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorJson("INVALID_REQUEST", "Erro ao processar requisição"));
        } else {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorJson("INVALID_REQUEST", message));
        }
    }

    private void handleError(Context ctx, String code, String message) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.json(createErrorJson(code, message));
    }

    private JsonObject createErrorJson(String code, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("error", code);
        json.addProperty("message", message);
        return json;
    }
}
