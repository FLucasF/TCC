package com.pousadamarealta.handler;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.util.DateUtil;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationHandler {
    private final ReservationService resService;
    private final MessageService msgService;
    private final ReservationRepository resRepo;
    private final RoomRepository roomRepo;

    public ReservationHandler(ReservationService resService, MessageService msgService, ReservationRepository resRepo, RoomRepository roomRepo) {
        this.resService = resService;
        this.msgService = msgService;
        this.resRepo = resRepo;
        this.roomRepo = roomRepo;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Integer roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = ((Number) body.get("guests")).intValue();
            String ratePlanStr = (String) body.get("ratePlan");

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);
            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation res = resService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(reservationToJson(res));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = resRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not_found"));
            ctx.json(reservationToJson(res));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

            Reservation res = resService.confirmReservation(id, amountPaid);

            // Send confirmation email
            String comprovante = generateComprovante(res);
            String emailBody = "Olá, " + res.guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                comprovante + "\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";
            msgService.sendEmail(res.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(reservationToJson(res));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = resService.cancelReservation(id);
            ctx.json(reservationToJson(res));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = resService.checkIn(id);
            ctx.json(reservationToJson(res));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = resService.checkOut(id);
            Room room = roomRepo.findByNumber(res.roomNumber).orElseThrow();
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("title", "Comprovante de reserva");
            document.put("reservationId", res.id);
            document.put("guestName", res.guestName);
            document.put("guestCpf", res.guestCpf);
            document.put("roomNumber", res.roomNumber);
            document.put("roomType", room.getTypeString());
            document.put("checkIn", DateUtil.formatDateISO(res.checkIn));
            document.put("checkOut", DateUtil.formatDateISO(res.checkOut));
            document.put("nights", res.nights);
            document.put("subtotal", res.subtotal);
            document.put("discount", res.discount);
            document.put("total", res.total);
            ctx.json(document);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation res = resRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not_found"));

            Room room = roomRepo.findByNumber(res.roomNumber).orElseThrow();
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("title", "Comprovante de reserva");
            document.put("reservationId", res.id);
            document.put("guestName", res.guestName);
            document.put("guestCpf", res.guestCpf);
            document.put("roomNumber", res.roomNumber);
            document.put("roomType", room.getTypeString());
            document.put("checkIn", DateUtil.formatDateISO(res.checkIn));
            document.put("checkOut", DateUtil.formatDateISO(res.checkOut));
            document.put("nights", res.nights);
            document.put("subtotal", res.subtotal);
            document.put("discount", res.discount);
            document.put("total", res.total);
            ctx.json(document);
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
        }
    }

    private String generateComprovante(Reservation res) {
        Room room = roomRepo.findByNumber(res.roomNumber).orElseThrow();
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.id).append("\n");
        sb.append("Hóspede: ").append(res.guestName).append(" (CPF ").append(res.guestCpf).append(")\n");
        sb.append("Quarto: ").append(res.roomNumber).append(" (").append(room.getTypeString()).append(")\n");
        sb.append("Entrada: ").append(DateUtil.formatDate(res.checkIn))
            .append(" | Saída: ").append(DateUtil.formatDate(res.checkOut))
            .append(" | ").append(res.nights).append(res.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: R$ ").append(res.subtotal).append("\n");
        if (res.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(res.discount).append("\n");
        }
        sb.append("Total: R$ ").append(res.total);
        return sb.toString();
    }

    private void handleError(Context ctx, String message) {
        if (message.contains("not_found")) {
            ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
        } else if (message.contains("Room not found")) {
            ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
        } else if (message.contains("Room not available")) {
            ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room unavailable"));
        } else if (message.contains("Invalid number of guests")) {
            ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Capacity exceeded"));
        } else if (message.contains("Cannot") || message.contains("Can only")) {
            ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", message));
        } else {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", message));
        }
    }

    private Map<String, Object> reservationToJson(Reservation res) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", res.id);
        map.put("roomNumber", res.roomNumber);
        map.put("guestName", res.guestName);
        map.put("guestEmail", res.guestEmail);
        map.put("guestCpf", res.guestCpf);
        map.put("checkIn", DateUtil.formatDateISO(res.checkIn));
        map.put("checkOut", DateUtil.formatDateISO(res.checkOut));
        map.put("guests", res.guests);
        map.put("ratePlan", res.ratePlan.toString());
        map.put("nights", res.nights);
        map.put("subtotal", res.subtotal);
        map.put("discount", res.discount);
        map.put("total", res.total);
        map.put("status", res.status.toString());
        map.put("amountPaid", res.amountPaid);
        map.put("refund", res.refund);
        return map;
    }
}
