package com.pousadamareaalta.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamareaalta.documents.DocumentGeneratorFactory;
import com.pousadamareaalta.models.*;
import com.pousadamareaalta.repositories.*;
import com.pousadamareaalta.utils.PriceCalculator;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReservationHandler {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageRepository messageRepository = new MessageRepository();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void create(Context ctx) {
        try {
            var body = ctx.bodyAsClass(Map.class);

            // Validate identification
            String guestCpf = (String) body.get("guestCpf");
            String companyCnpj = (String) body.get("companyCnpj");
            String companyName = (String) body.get("companyName");
            String guestPassport = (String) body.get("guestPassport");
            String guestCountry = (String) body.get("guestCountry");

            int identificationCount = 0;
            if (guestCpf != null) identificationCount++;
            if (companyCnpj != null) identificationCount++;
            if (guestPassport != null) identificationCount++;

            if (identificationCount != 1) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "Must provide exactly one identification method";
                });
                return;
            }

            if (companyCnpj != null && companyName == null) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "companyName required when companyCnpj is provided";
                });
                return;
            }

            if (guestPassport != null && guestCountry == null) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "guestCountry required when guestPassport is provided";
                });
                return;
            }

            // Parse required fields
            int roomNumber;
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            LocalDate checkIn;
            LocalDate checkOut;
            int guests;
            String ratePlan = (String) body.get("ratePlan");

            try {
                roomNumber = ((Number) body.get("roomNumber")).intValue();
                checkIn = LocalDate.parse((String) body.get("checkIn"));
                checkOut = LocalDate.parse((String) body.get("checkOut"));
                guests = ((Number) body.get("guests")).intValue();
            } catch (Exception e) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "Invalid input format";
                });
                return;
            }

            // Validate room exists
            Room room = roomRepository.findByNumber(roomNumber);
            if (room == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "ROOM_NOT_FOUND";
                    public final String message = "Room not found";
                });
                return;
            }

            // Validate dates
            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(new Object() {
                    public final String error = "INVALID_REQUEST";
                    public final String message = "Check-out must be after check-in";
                });
                return;
            }

            // Validate guests
            if (guests < 1 || guests > room.capacity) {
                int statusCode = guests < 1 ? 400 : 422;
                String errorCode = guests < 1 ? "INVALID_REQUEST" : "CAPACITY_EXCEEDED";
                String finalErrorCode = errorCode;
                ctx.status(statusCode).json(new Object() {
                    public final String error = finalErrorCode;
                    public final String message = "Invalid number of guests";
                });
                return;
            }

            // Check room availability
            var conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
            if (!conflicts.isEmpty()) {
                ctx.status(409).json(new Object() {
                    public final String error = "ROOM_UNAVAILABLE";
                    public final String message = "Room is not available for the selected dates";
                });
                return;
            }

            // Calculate prices
            int nights = PriceCalculator.calculateNights(checkIn, checkOut);
            double subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, room.nightlyRate);
            double discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
            double total = PriceCalculator.round(subtotal - discount);

            // Create reservation
            Reservation reservation = new Reservation(
                    roomNumber, guestName, guestEmail,
                    guestCpf, companyCnpj, companyName,
                    guestPassport, guestCountry,
                    checkIn, checkOut, guests, ratePlan,
                    nights, subtotal, discount, total
            );

            long reservationId = reservationRepository.create(reservation);
            reservation.id = reservationId;

            ctx.status(201).json(buildReservationResponse(reservation));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void get(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            ctx.json(buildReservationResponse(reservation));
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void confirm(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            if (!"PENDING".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Only pending reservations can be confirmed";
                });
                return;
            }

            reservation.status = "CONFIRMED";
            reservation.amountPaid = reservation.total;
            reservationRepository.update(reservation);

            // Send confirmation email
            sendConfirmationEmail(reservation, id);

            ctx.json(buildReservationResponse(reservation));
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void cancel(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            if ("CANCELLED".equals(reservation.status) || "CHECKED_OUT".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Cannot cancel this reservation";
                });
                return;
            }

            if ("CHECKED_IN".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Cannot cancel a checked-in reservation";
                });
                return;
            }

            String previousStatus = reservation.status;
            reservation.status = "CANCELLED";

            if ("CONFIRMED".equals(previousStatus) && "STANDARD".equals(reservation.ratePlan)) {
                reservation.refund = reservation.amountPaid;
            } else {
                reservation.refund = 0.0;
            }

            reservationRepository.update(reservation);

            ctx.json(buildReservationResponse(reservation));
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            if (!"CONFIRMED".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Only confirmed reservations can check-in";
                });
                return;
            }

            reservation.status = "CHECKED_IN";
            reservationRepository.update(reservation);

            ctx.json(buildReservationResponse(reservation));
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            if (!"CHECKED_IN".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Only checked-in reservations can check-out";
                });
                return;
            }

            reservation.status = "CHECKED_OUT";
            reservationRepository.update(reservation);

            Room room = roomRepository.findByNumber(reservation.roomNumber);
            Document document = generateDocument(reservation, room, id);

            ctx.json(document);
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id);

            if (reservation == null) {
                ctx.status(404).json(new Object() {
                    public final String error = "RESERVATION_NOT_FOUND";
                    public final String message = "Reservation not found";
                });
                return;
            }

            if ("PENDING".equals(reservation.status)) {
                ctx.status(409).json(new Object() {
                    public final String error = "OPERATION_NOT_ALLOWED";
                    public final String message = "Document only available for paid reservations";
                });
                return;
            }

            Room room = roomRepository.findByNumber(reservation.roomNumber);
            Document document = generateDocument(reservation, room, id);

            ctx.json(document);
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }

    private Document generateDocument(Reservation reservation, Room room, long reservationId) {
        var generator = DocumentGeneratorFactory.create(reservation);
        String text = generator.generate(reservation, room, reservationId);
        String title = generator.getTitle();

        Document doc = new Document(
                reservationId, reservation.guestName, reservation.guestCpf,
                reservation.companyCnpj, reservation.companyName,
                reservation.guestPassport, reservation.guestCountry,
                room.number, room.type,
                formatDateForJson(reservation.checkIn),
                formatDateForJson(reservation.checkOut),
                reservation.nights,
                reservation.subtotal, reservation.discount, reservation.total,
                text
        );
        doc.title = title;
        return doc;
    }

    private String formatDateForJson(LocalDate date) {
        return date.toString();
    }

    private void sendConfirmationEmail(Reservation reservation, long reservationId) throws Exception {
        Room room = roomRepository.findByNumber(reservation.roomNumber);
        var generator = DocumentGeneratorFactory.create(reservation);
        String documentText = generator.generate(reservation, room, reservationId);

        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.guestName, documentText);

        Message message = new Message("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);
        messageRepository.save(message);
    }

    private Object buildReservationResponse(Reservation reservation) {
        return new Object() {
            public final Long id = reservation.id;
            public final int roomNumber = reservation.roomNumber;
            public final String guestName = reservation.guestName;
            public final String guestEmail = reservation.guestEmail;
            public final String guestCpf = reservation.guestCpf;
            public final String companyCnpj = reservation.companyCnpj;
            public final String companyName = reservation.companyName;
            public final String guestPassport = reservation.guestPassport;
            public final String guestCountry = reservation.guestCountry;
            public final String checkIn = formatDateForJson(reservation.checkIn);
            public final String checkOut = formatDateForJson(reservation.checkOut);
            public final int guests = reservation.guests;
            public final String ratePlan = reservation.ratePlan;
            public final int nights = reservation.nights;
            public final double subtotal = reservation.subtotal;
            public final double discount = reservation.discount;
            public final double total = reservation.total;
            public final String status = reservation.status;
            public final double amountPaid = reservation.amountPaid;
            public final Double refund = reservation.refund;
        };
    }
}
