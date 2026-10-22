package br.com.marealta.web;

import br.com.marealta.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Lê e valida o que chega do site; qualquer problema vira INVALID_REQUEST. */
final class RequestReader {
    private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final ObjectMapper mapper;

    RequestReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation newReservation(String body) {
        JsonNode json = parseObject(body);
        LocalDate checkIn = date(requiredText(json, "checkIn"), "checkIn");
        LocalDate checkOut = date(requiredText(json, "checkOut"), "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw DomainException.invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw DomainException.invalid("É preciso pelo menos um hóspede");
        }
        IdentityFields fields = new IdentityFields(optionalText(json, "guestCpf"), optionalText(json, "companyCnpj"),
                optionalText(json, "companyName"), optionalText(json, "guestPassport"),
                optionalText(json, "guestCountry"));
        return new NewReservation(integer(json, "roomNumber"), requiredText(json, "guestName"),
                requiredText(json, "guestEmail"), IdentityKinds.from(fields), checkIn, checkOut, guests,
                ratePlan(requiredText(json, "ratePlan")));
    }

    LocalDate night(String value) {
        return date(value, "date");
    }

    YearMonth month(String value) {
        if (value == null || !MONTH.matcher(value).matches()) {
            throw DomainException.invalid("Mês inválido, use o formato 2027-07");
        }
        return YearMonth.parse(value);
    }

    private JsonNode parseObject(String body) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null || !node.isObject()) {
                throw DomainException.invalid("O corpo deve ser um objeto JSON");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw DomainException.invalid("O corpo não é um JSON válido");
        }
    }

    private static String optionalText(JsonNode json, String name) {
        JsonNode value = json.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw DomainException.invalid("Campo deve ser texto: " + name);
        }
        return value.asText();
    }

    private static String requiredText(JsonNode json, String name) {
        return IdentityKind.require(optionalText(json, name), name);
    }

    private static int integer(JsonNode json, String name) {
        JsonNode value = json.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw DomainException.invalid("Campo deve ser um número inteiro: " + name);
        }
        return value.asInt();
    }

    private static LocalDate date(String value, String name) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Data inválida em " + name + ", use o formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw DomainException.invalid("Plano de tarifa inválido: " + value);
        }
    }
}
