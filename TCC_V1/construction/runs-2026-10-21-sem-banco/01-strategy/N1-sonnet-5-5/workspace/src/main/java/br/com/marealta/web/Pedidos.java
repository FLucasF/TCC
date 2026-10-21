package br.com.marealta.web;

import br.com.marealta.dominio.ApiException;
import br.com.marealta.servico.PedidoDeReserva;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Leitura e validação do formato dos dados que chegam do site. */
final class Pedidos {

    private Pedidos() {
    }

    static PedidoDeReserva reserva(ObjectMapper json, String corpo) {
        JsonNode raiz;
        try {
            raiz = json.readTree(corpo);
        } catch (JsonProcessingException e) {
            throw ApiException.invalida("O corpo não é um JSON válido");
        }
        if (raiz == null || !raiz.isObject()) {
            throw ApiException.invalida("O corpo deve ser um objeto JSON");
        }
        return new PedidoDeReserva(
                inteiro(raiz, "roomNumber"),
                texto(raiz, "guestName"),
                texto(raiz, "guestEmail"),
                texto(raiz, "guestCpf"),
                data(texto(raiz, "checkIn")),
                data(texto(raiz, "checkOut")),
                inteiro(raiz, "guests"),
                texto(raiz, "ratePlan"),
                textoOpcional(raiz, "companyCode"));
    }

    static LocalDate data(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalida("Data fora do formato AAAA-MM-DD");
        }
    }

    static YearMonth mes(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalida("Mês fora do formato AAAA-MM");
        }
    }

    private static String texto(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        if (valor == null || !valor.isTextual() || valor.asText().isBlank()) {
            throw ApiException.invalida("Campo obrigatório ausente ou inválido: " + campo);
        }
        return valor.asText();
    }

    private static String textoOpcional(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        return valor != null && valor.isTextual() ? valor.asText() : null;
    }

    private static int inteiro(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        if (valor == null || !valor.isInt()) {
            throw ApiException.invalida("Campo obrigatório ausente ou inválido: " + campo);
        }
        return valor.asInt();
    }
}
