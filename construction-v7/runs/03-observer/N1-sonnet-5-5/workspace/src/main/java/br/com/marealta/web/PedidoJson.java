package br.com.marealta.web;

import br.com.marealta.dominio.CodigoErro;
import br.com.marealta.dominio.ErroNegocio;
import br.com.marealta.dominio.NovaReserva;
import br.com.marealta.dominio.PlanoTarifa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations. */
final class PedidoJson {

    private PedidoJson() {
    }

    static NovaReserva ler(ObjectMapper mapper, String corpo) {
        JsonNode json;
        try {
            json = mapper.readTree(corpo);
        } catch (Exception e) {
            throw invalido("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalido("O corpo deve ser um objeto JSON");
        }
        return new NovaReserva(
                inteiro(json, "roomNumber"),
                texto(json, "guestName"),
                texto(json, "guestEmail"),
                texto(json, "guestCpf"),
                data(json, "checkIn"),
                data(json, "checkOut"),
                inteiro(json, "guests"),
                plano(json));
    }

    private static String texto(JsonNode json, String campo) {
        JsonNode no = json.get(campo);
        if (no == null || !no.isTextual() || no.asText().isBlank()) {
            throw invalido("Campo '" + campo + "' ausente ou inválido");
        }
        return no.asText();
    }

    private static int inteiro(JsonNode json, String campo) {
        JsonNode no = json.get(campo);
        if (no == null || !no.isIntegralNumber() || !no.canConvertToInt()) {
            throw invalido("Campo '" + campo + "' ausente ou inválido");
        }
        return no.asInt();
    }

    private static LocalDate data(JsonNode json, String campo) {
        try {
            return LocalDate.parse(texto(json, campo));
        } catch (DateTimeParseException e) {
            throw invalido("Campo '" + campo + "' deve estar no formato AAAA-MM-DD");
        }
    }

    private static PlanoTarifa plano(JsonNode json) {
        try {
            return PlanoTarifa.valueOf(texto(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalido("Campo 'ratePlan' deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static ErroNegocio invalido(String mensagem) {
        return new ErroNegocio(CodigoErro.INVALID_REQUEST, mensagem);
    }
}
