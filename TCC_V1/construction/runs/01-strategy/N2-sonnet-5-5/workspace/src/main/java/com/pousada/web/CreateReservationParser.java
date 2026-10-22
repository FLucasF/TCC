package com.pousada.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.error.ApiException;
import com.pousada.plan.RatePlans;
import com.pousada.service.CreateReservation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Valida o corpo do POST /reservations e o transforma em um comando. */
public class CreateReservationParser {

    private final ObjectMapper mapper;
    private final RatePlans plans;

    public CreateReservationParser(ObjectMapper mapper, RatePlans plans) {
        this.mapper = mapper;
        this.plans = plans;
    }

    public CreateReservation parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        String ratePlan = text(json, "ratePlan");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso ao menos um hóspede");
        }
        if (plans.find(ratePlan).isEmpty()) {
            throw ApiException.invalidRequest("Plano de tarifa desconhecido: " + ratePlan);
        }
        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;
        return new CreateReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, ratePlan, companyCode);
    }

    private String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + ", use AAAA-MM-DD");
        }
    }
}
