package com.pousada.api;

import com.pousada.domain.Reservation;
import com.pousada.document.Document;
import org.json.JSONObject;
import java.math.BigDecimal;

public class JsonUtil {
    public static JSONObject reservationToJson(Reservation res) {
        JSONObject json = new JSONObject();
        json.put("id", res.getId());
        json.put("roomNumber", res.getRoomNumber());
        json.put("guestName", res.getGuestName());
        json.put("guestEmail", res.getGuestEmail());
        json.put("guestCpf", res.getGuestCpf());
        json.put("companyCnpj", res.getCompanyCnpj());
        json.put("companyName", res.getCompanyName());
        json.put("guestPassport", res.getGuestPassport());
        json.put("guestCountry", res.getGuestCountry());
        json.put("checkIn", res.getCheckIn().toString());
        json.put("checkOut", res.getCheckOut().toString());
        json.put("guests", res.getGuests());
        json.put("ratePlan", res.getRatePlan().name());
        json.put("nights", res.getNights());
        json.put("subtotal", formatDecimal(res.getSubtotal()));
        json.put("discount", formatDecimal(res.getDiscount()));
        json.put("total", formatDecimal(res.getTotal()));
        json.put("status", res.getStatus().name());
        json.put("amountPaid", formatDecimal(res.getAmountPaid()));
        json.put("refund", res.getRefund() != null ? formatDecimal(res.getRefund()) : JSONObject.NULL);
        return json;
    }

    public static JSONObject documentToJson(Document doc) {
        JSONObject json = new JSONObject();
        json.put("title", doc.title());
        json.put("reservationId", doc.reservationId());
        json.put("guestName", doc.guestName());
        json.put("guestCpf", doc.guestCpf());
        json.put("companyCnpj", doc.companyCnpj());
        json.put("companyName", doc.companyName());
        json.put("guestPassport", doc.guestPassport());
        json.put("guestCountry", doc.guestCountry());
        json.put("roomNumber", doc.roomNumber());
        json.put("roomType", doc.roomType());
        json.put("checkIn", doc.checkIn().toString());
        json.put("checkOut", doc.checkOut().toString());
        json.put("nights", doc.nights());
        json.put("subtotal", formatDecimal(doc.subtotal()));
        json.put("discount", doc.discount() != null ? formatDecimal(doc.discount()) : JSONObject.NULL);
        json.put("total", formatDecimal(doc.total()));
        json.put("text", doc.text());
        return json;
    }

    public static double formatDecimal(BigDecimal value) {
        return value.doubleValue();
    }

    public static JSONObject error(String code, String message) {
        JSONObject json = new JSONObject();
        json.put("error", code);
        json.put("message", message);
        return json;
    }
}
