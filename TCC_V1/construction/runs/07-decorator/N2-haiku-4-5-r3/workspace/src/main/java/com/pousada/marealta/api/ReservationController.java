package com.pousada.marealta.api;

import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Document;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.infrastructure.MessageRepository;
import com.pousada.marealta.infrastructure.ReservationRepository;
import com.pousada.marealta.infrastructure.RoomRepository;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.PriceCalculator;
import com.pousada.marealta.service.PricingStrategy;
import com.pousada.marealta.service.ReservationState;
import com.pousada.marealta.service.ReservationStateMachine;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReservationController {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(ReservationRepository reservationRepository,
                                RoomRepository roomRepository,
                                MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public void createReservation(Context ctx) throws Exception {
        String bodyStr = ctx.body();
        JsonObject body = com.google.gson.JsonParser.parseString(bodyStr).getAsJsonObject();

        if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
            !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
            !body.has("guests") || !body.has("ratePlan")) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Missing required fields");
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(error);
            return;
        }

        int roomNumber = body.getAsJsonPrimitive("roomNumber").getAsInt();
        String guestName = body.getAsJsonPrimitive("guestName").getAsString();
        String guestEmail = body.getAsJsonPrimitive("guestEmail").getAsString();
        String guestCpf = body.getAsJsonPrimitive("guestCpf").getAsString();
        LocalDate checkIn = LocalDate.parse(body.getAsJsonPrimitive("checkIn").getAsString(), DATE_FORMATTER);
        LocalDate checkOut = LocalDate.parse(body.getAsJsonPrimitive("checkOut").getAsString(), DATE_FORMATTER);
        int guests = body.getAsJsonPrimitive("guests").getAsInt();
        String ratePlan = body.getAsJsonPrimitive("ratePlan").getAsString();

        Optional<Room> room = roomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "ROOM_NOT_FOUND");
            error.addProperty("message", "Room not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        if (guests < 1 || guests > room.get().getCapacity()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "CAPACITY_EXCEEDED");
            error.addProperty("message", "Number of guests exceeds room capacity");
            ctx.status(422);
            ctx.json(error);
            return;
        }

        if (!checkIn.isBefore(checkOut)) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Check-in must be before check-out");
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(error);
            return;
        }

        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "ROOM_UNAVAILABLE");
            error.addProperty("message", "Room is not available for the selected dates");
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        PricingStrategy pricingStrategy = "NON_REFUNDABLE".equals(ratePlan) ?
            new com.pousada.marealta.service.NonRefundablePricingStrategy() :
            new com.pousada.marealta.service.StandardPricingStrategy();
        PriceCalculator priceCalculator = new PriceCalculator(pricingStrategy);
        PriceCalculator.PriceBreakdown pricing = priceCalculator.calculatePrice(
            room.get(), checkIn, checkOut);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(RatePlan.valueOf(ratePlan));
        reservation.setNights(pricing.nights);
        reservation.setSubtotal(pricing.subtotal);
        reservation.setDiscount(pricing.discount);
        reservation.setTotal(pricing.total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        reservation = reservationRepository.create(reservation);

        ctx.status(HttpStatus.CREATED);
        ctx.json(mapReservationToJson(reservation));
    }

    public void getReservation(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        ctx.json(mapReservationToJson(reservation.get()));
    }

    public void confirmReservation(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        Reservation res = reservation.get();
        ReservationState state = ReservationStateMachine.getState(res.getStatus());

        try {
            res = state.confirm(res);
            reservationRepository.update(res);

            Message message = createConfirmationEmail(res);
            messageService.sendAndStoreMessage(message);
        } catch (IllegalStateException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "OPERATION_NOT_ALLOWED");
            error.addProperty("message", e.getMessage());
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        ctx.json(mapReservationToJson(res));
    }

    public void cancelReservation(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        Reservation res = reservation.get();
        ReservationState state = ReservationStateMachine.getState(res.getStatus());

        try {
            res = state.cancel(res);
            reservationRepository.update(res);
        } catch (IllegalStateException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "OPERATION_NOT_ALLOWED");
            error.addProperty("message", e.getMessage());
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        ctx.json(mapReservationToJson(res));
    }

    public void checkIn(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        Reservation res = reservation.get();
        ReservationState state = ReservationStateMachine.getState(res.getStatus());

        try {
            res = state.checkIn(res);
            reservationRepository.update(res);
        } catch (IllegalStateException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "OPERATION_NOT_ALLOWED");
            error.addProperty("message", e.getMessage());
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        ctx.json(mapReservationToJson(res));
    }

    public void checkOut(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        Reservation res = reservation.get();
        ReservationState state = ReservationStateMachine.getState(res.getStatus());

        try {
            res = state.checkOut(res);
            reservationRepository.update(res);
        } catch (IllegalStateException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "OPERATION_NOT_ALLOWED");
            error.addProperty("message", e.getMessage());
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        ctx.json(mapDocumentToJson(createDocument(res)));
    }

    public void getDocument(Context ctx) throws Exception {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "RESERVATION_NOT_FOUND");
            error.addProperty("message", "Reservation not found");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(error);
            return;
        }

        if (reservation.get().getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "OPERATION_NOT_ALLOWED");
            error.addProperty("message", "No document available for unpaid reservations");
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(error);
            return;
        }

        ctx.json(mapDocumentToJson(createDocument(reservation.get())));
    }

    private Message createConfirmationEmail(Reservation res) throws Exception {
        Optional<Room> room = roomRepository.findByNumber(res.getRoomNumber());
        String roomType = room.map(r -> r.getType().getLabel()).orElse("UNKNOWN");

        Document doc = createDocument(res);
        String docText = formatDocument(doc);

        String body = """
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!
                """.formatted(res.getGuestName(), docText);

        return new Message("EMAIL", res.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta", body, MessageStatus.SENT);
    }

    private Document createDocument(Reservation res) throws Exception {
        Optional<Room> room = roomRepository.findByNumber(res.getRoomNumber());
        String roomType = room.map(r -> r.getType().getLabel()).orElse("UNKNOWN");

        return new Document(res.getId(), res.getGuestName(), res.getGuestCpf(),
            res.getRoomNumber(), roomType, res.getCheckIn(), res.getCheckOut(),
            res.getNights(), res.getSubtotal(), res.getDiscount(), res.getTotal());
    }

    private String formatDocument(Document doc) {
        String nightsLabel = doc.getNights() == 1 ? "1 diária" : doc.getNights() + " diárias";
        String discountLine = doc.getDiscount().compareTo(BigDecimal.ZERO) > 0 ?
            "Desconto: R$ " + formatCurrency(doc.getDiscount()) + "\n" : "";

        return """
                Comprovante de reserva nº %d
                Hóspede: %s (CPF %s)
                Quarto: %d (%s)
                Entrada: %s | Saída: %s | %s
                Diárias: R$ %s
                %sTotal: R$ %s
                """.formatted(
                doc.getReservationId(),
                doc.getGuestName(),
                formatCPF(doc.getGuestCpf()),
                doc.getRoomNumber(),
                doc.getRoomType(),
                formatDate(doc.getCheckIn()),
                formatDate(doc.getCheckOut()),
                nightsLabel,
                formatCurrency(doc.getSubtotal()),
                discountLine,
                formatCurrency(doc.getTotal()));
    }

    private String formatCurrency(BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }

    private String formatCPF(String cpf) {
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private JsonObject mapReservationToJson(Reservation res) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", res.getId());
        obj.addProperty("roomNumber", res.getRoomNumber());
        obj.addProperty("guestName", res.getGuestName());
        obj.addProperty("guestEmail", res.getGuestEmail());
        obj.addProperty("guestCpf", res.getGuestCpf());
        obj.addProperty("checkIn", res.getCheckIn().toString());
        obj.addProperty("checkOut", res.getCheckOut().toString());
        obj.addProperty("guests", res.getGuests());
        obj.addProperty("ratePlan", res.getRatePlan().getLabel());
        obj.addProperty("nights", res.getNights());
        obj.addProperty("subtotal", res.getSubtotal().doubleValue());
        obj.addProperty("discount", res.getDiscount().doubleValue());
        obj.addProperty("total", res.getTotal().doubleValue());
        obj.addProperty("status", res.getStatus().name());
        obj.addProperty("amountPaid", res.getAmountPaid().doubleValue());
        if (res.getRefund() != null) {
            obj.addProperty("refund", res.getRefund().doubleValue());
        } else {
            obj.add("refund", null);
        }
        return obj;
    }

    private JsonObject mapDocumentToJson(Document doc) {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", doc.getTitle());
        obj.addProperty("reservationId", doc.getReservationId());
        obj.addProperty("guestName", doc.getGuestName());
        obj.addProperty("guestCpf", doc.getGuestCpf());
        obj.addProperty("roomNumber", doc.getRoomNumber());
        obj.addProperty("roomType", doc.getRoomType());
        obj.addProperty("checkIn", doc.getCheckIn().toString());
        obj.addProperty("checkOut", doc.getCheckOut().toString());
        obj.addProperty("nights", doc.getNights());
        obj.addProperty("subtotal", doc.getSubtotal().doubleValue());
        obj.addProperty("discount", doc.getDiscount().doubleValue());
        obj.addProperty("total", doc.getTotal().doubleValue());
        return obj;
    }
}
