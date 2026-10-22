package br.com.pousadamareal.api.dto;

import br.com.pousadamareal.domain.Reservation;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class ReservationDTO {
    public static JsonObject toJson(Reservation reservation) {
        JsonObject json = new JsonObject();
        json.addProperty("id", reservation.getId());
        json.addProperty("roomNumber", reservation.getRoomNumber());
        json.addProperty("guestName", reservation.getGuestName());
        json.addProperty("guestEmail", reservation.getGuestEmail());
        addNullableProperty(json, "guestCpf", reservation.getGuestCpf());
        addNullableProperty(json, "companyCnpj", reservation.getCompanyCnpj());
        addNullableProperty(json, "companyName", reservation.getCompanyName());
        addNullableProperty(json, "guestPassport", reservation.getGuestPassport());
        addNullableProperty(json, "guestCountry", reservation.getGuestCountry());
        json.addProperty("checkIn", reservation.getCheckIn().toString());
        json.addProperty("checkOut", reservation.getCheckOut().toString());
        json.addProperty("guests", reservation.getGuests());
        json.addProperty("ratePlan", reservation.getRatePlan().getValue());
        json.addProperty("nights", reservation.getNights());
        json.addProperty("subtotal", Math.round(reservation.getSubtotal() * 100) / 100.0);
        json.addProperty("discount", Math.round(reservation.getDiscount() * 100) / 100.0);
        json.addProperty("total", Math.round(reservation.getTotal() * 100) / 100.0);
        json.addProperty("status", reservation.getStatus().getValue());
        json.addProperty("amountPaid", Math.round(reservation.getAmountPaid() * 100) / 100.0);
        if (reservation.getRefund() != null) {
            json.addProperty("refund", Math.round(reservation.getRefund() * 100) / 100.0);
        } else {
            json.add("refund", JsonNull.INSTANCE);
        }
        return json;
    }

    private static void addNullableProperty(JsonObject json, String key, String value) {
        if (value != null) {
            json.addProperty(key, value);
        } else {
            json.add(key, JsonNull.INSTANCE);
        }
    }

    public static JsonObject errorResponse(String code, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("error", code);
        json.addProperty("message", message);
        return json;
    }
}
