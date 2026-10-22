package br.com.pousadamareal.api.dto;

import br.com.pousadamareal.domain.Document;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class DocumentDTO {
    public static JsonObject toJson(Document document) {
        JsonObject json = new JsonObject();
        json.addProperty("title", document.getTitle());
        json.addProperty("reservationId", document.getReservationId());
        json.addProperty("guestName", document.getGuestName());
        addNullableProperty(json, "guestCpf", document.getGuestCpf());
        addNullableProperty(json, "companyCnpj", document.getCompanyCnpj());
        addNullableProperty(json, "companyName", document.getCompanyName());
        addNullableProperty(json, "guestPassport", document.getGuestPassport());
        addNullableProperty(json, "guestCountry", document.getGuestCountry());
        json.addProperty("roomNumber", document.getRoomNumber());
        json.addProperty("roomType", document.getRoomType());
        json.addProperty("checkIn", document.getCheckIn());
        json.addProperty("checkOut", document.getCheckOut());
        json.addProperty("nights", document.getNights());
        json.addProperty("subtotal", Math.round(document.getSubtotal() * 100) / 100.0);
        if (document.getDiscount() != null) {
            json.addProperty("discount", Math.round(document.getDiscount() * 100) / 100.0);
        } else {
            json.add("discount", JsonNull.INSTANCE);
        }
        json.addProperty("total", Math.round(document.getTotal() * 100) / 100.0);
        json.addProperty("text", document.getText());
        return json;
    }

    private static void addNullableProperty(JsonObject json, String key, String value) {
        if (value != null) {
            json.addProperty(key, value);
        } else {
            json.add(key, JsonNull.INSTANCE);
        }
    }
}
