package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.Reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.LinkedHashMap;
import java.util.Map;

final class Json {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Json() {
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.room().number());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.put("guestCpf", r.guestCpf());
        json.put("checkIn", r.checkIn());
        json.put("checkOut", r.checkOut());
        json.put("guests", r.guests());
        json.put("ratePlan", r.ratePlan());
        json.put("nights", r.quote().nights());
        json.put("subtotal", r.quote().subtotal());
        json.put("discount", r.quote().discount());
        json.put("total", r.quote().total());
        json.put("status", r.status());
        json.put("amountPaid", r.amountPaid());
        json.put("refund", r.refund());
        return json;
    }
}
