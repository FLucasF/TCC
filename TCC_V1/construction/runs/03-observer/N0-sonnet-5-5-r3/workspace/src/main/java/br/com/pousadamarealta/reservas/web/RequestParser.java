package br.com.pousadamarealta.reservas.web;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.service.NewReservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Valida e converte as entradas da API; qualquer defeito vira INVALID_REQUEST. */
final class RequestParser {
    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation newReservation(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo da requisição não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo da requisição deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String guestName = text(json, "guestName");
        String guestEmail = text(json, "guestEmail");
        String guestCpf = text(json, "guestCpf");
        LocalDate checkIn = date(text(json, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(json, "checkOut"), "checkOut");
        int guests = integer(json, "guests");
        String planName = text(json, "ratePlan");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("checkOut deve ser depois de checkIn");
        }
        if (guests < 1) {
            throw invalid("guests deve ser pelo menos 1");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(planName);
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        return new NewReservation(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, plan);
    }

    LocalDate queryDate(String value, String name) {
        if (value == null) {
            throw invalid("Informe " + name);
        }
        return date(value, name);
    }

    YearMonth queryMonth(String value, String name) {
        if (value == null) {
            throw invalid("Informe " + name);
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(name + " deve estar no formato AAAA-MM");
        }
    }

    private static LocalDate date(String value, String name) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(name + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.intValue();
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
