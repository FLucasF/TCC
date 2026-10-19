package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.domain.*;
import com.pousada.exception.ApiException;
import com.pousada.service.ReservationService;
import com.pousada.util.DateHelper;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final Gson gson = new Gson();

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            String body = ctx.body();
            if (body == null || body.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Request body is required"));
                return;
            }

            JsonObject jsonBody = gson.fromJson(body, JsonObject.class);
            if (jsonBody == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON"));
                return;
            }

            Integer roomNumber = getIntFromJson(jsonBody, "roomNumber");
            String guestName = getStringFromJson(jsonBody, "guestName");
            String guestEmail = getStringFromJson(jsonBody, "guestEmail");
            String guestCpf = getStringFromJson(jsonBody, "guestCpf");
            String checkInStr = getStringFromJson(jsonBody, "checkIn");
            String checkOutStr = getStringFromJson(jsonBody, "checkOut");
            Integer guests = getIntFromJson(jsonBody, "guests");
            String ratePlanStr = getStringFromJson(jsonBody, "ratePlan");
            String companyCode = jsonBody.has("companyCode") ? jsonBody.get("companyCode").getAsString() : null;

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = DateHelper.parseDate(checkInStr);
                checkOut = DateHelper.parseDate(checkOutStr);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format. Use yyyy-MM-dd"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(ratePlanStr);
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan, companyCode
            );

            ctx.status(201);
            ctx.json(toReservationResponse(reservation));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument doc = reservationService.checkOut(id);
            ctx.json(toDocumentResponse(doc));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument doc = reservationService.getDocument(id);
            ctx.json(toDocumentResponse(doc));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }

    private ReservationResponse toReservationResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getRoomNumber(),
                r.getGuestName(),
                r.getGuestEmail(),
                r.getGuestCpf(),
                DateHelper.formatDateIso(r.getCheckIn()),
                DateHelper.formatDateIso(r.getCheckOut()),
                r.getGuests(),
                r.getRatePlan().toString(),
                r.getNights(),
                r.getSubtotal(),
                r.getDiscount(),
                r.getTotal(),
                r.getStatus().toString(),
                r.getAmountPaid(),
                r.getRefund()
        );
    }

    private DocumentResponse toDocumentResponse(ReservationDocument doc) {
        return new DocumentResponse(
                doc.getTitle(),
                doc.getReservationId(),
                doc.getGuestName(),
                doc.getGuestCpf(),
                doc.getRoomNumber(),
                doc.getRoomType(),
                DateHelper.formatDateIso(doc.getCheckIn()),
                DateHelper.formatDateIso(doc.getCheckOut()),
                doc.getNights(),
                doc.getSubtotal(),
                doc.getDiscount(),
                doc.getTotal()
        );
    }

    private Integer getIntFromJson(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringFromJson(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public static class ReservationResponse {
        public final long id;
        public final int roomNumber;
        public final String guestName;
        public final String guestEmail;
        public final String guestCpf;
        public final String checkIn;
        public final String checkOut;
        public final int guests;
        public final String ratePlan;
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;
        public final String status;
        public final BigDecimal amountPaid;
        public final BigDecimal refund;

        public ReservationResponse(long id, int roomNumber, String guestName, String guestEmail,
                                  String guestCpf, String checkIn, String checkOut, int guests,
                                  String ratePlan, int nights, BigDecimal subtotal, BigDecimal discount,
                                  BigDecimal total, String status, BigDecimal amountPaid, BigDecimal refund) {
            this.id = id;
            this.roomNumber = roomNumber;
            this.guestName = guestName;
            this.guestEmail = guestEmail;
            this.guestCpf = guestCpf;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.guests = guests;
            this.ratePlan = ratePlan;
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.status = status;
            this.amountPaid = amountPaid;
            this.refund = refund;
        }
    }

    public static class DocumentResponse {
        public final String title;
        public final long reservationId;
        public final String guestName;
        public final String guestCpf;
        public final int roomNumber;
        public final String roomType;
        public final String checkIn;
        public final String checkOut;
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public DocumentResponse(String title, long reservationId, String guestName, String guestCpf,
                               int roomNumber, String roomType, String checkIn, String checkOut,
                               int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.title = title;
            this.reservationId = reservationId;
            this.guestName = guestName;
            this.guestCpf = guestCpf;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static class ErrorResponse {
        public final String error;
        public final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
