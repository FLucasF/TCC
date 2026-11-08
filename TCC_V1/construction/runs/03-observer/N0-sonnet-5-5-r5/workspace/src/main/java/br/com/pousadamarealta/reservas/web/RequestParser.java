package br.com.pousadamarealta.reservas.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.pousadamarealta.reservas.domain.PousadaException;
import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.service.CreateReservation;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.INVALID_REQUEST;

/** Valida o formato do que o site manda; tudo que está errado aqui é 400 INVALID_REQUEST. */
final class RequestParser {
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 9999;

    private RequestParser() {}

    static CreateReservation createReservation(ObjectMapper mapper, byte[] body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (IOException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("checkOut deve ser depois de checkIn");
        }
        if (guests < 1) {
            throw invalid("guests deve ser pelo menos 1");
        }
        return new CreateReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    static LocalDate queryDate(String value) {
        if (value == null) {
            throw invalid("Informe date no formato AAAA-MM-DD");
        }
        return parseDate(value, "date");
    }

    static YearMonth queryMonth(String value) {
        if (value == null) {
            throw invalid("Informe month no formato AAAA-MM");
        }
        try {
            YearMonth month = YearMonth.parse(value);
            checkYear(month.getYear(), "month");
            return month;
        } catch (DateTimeException e) {
            throw invalid("month deve estar no formato AAAA-MM");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("O campo " + field + " é obrigatório");
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("O campo " + field + " é obrigatório e deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        return parseDate(text(json, field), field);
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            LocalDate date = LocalDate.parse(value);
            checkYear(date.getYear(), field);
            return date;
        } catch (DateTimeException e) {
            throw invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static void checkYear(int year, String field) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw invalid(field + " fora do intervalo aceito");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(INVALID_REQUEST, message);
    }
}
