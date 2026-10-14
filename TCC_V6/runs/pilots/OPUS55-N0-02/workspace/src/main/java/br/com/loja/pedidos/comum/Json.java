package br.com.loja.pedidos.comum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Leitura tolerante de valores vindos de um JSON genérico (mapas, listas, textos e números). */
public final class Json {

    private Json() {
    }

    /** Texto não vazio, ou {@code null} se ausente, vazio ou de outro tipo. */
    public static String texto(Object valor) {
        return valor instanceof String s && !s.isBlank() ? s : null;
    }

    /** Número inteiro, ou {@code null} se ausente, fracionário ou de outro tipo. */
    public static Long inteiro(Object valor) {
        try {
            if (valor instanceof Integer || valor instanceof Long || valor instanceof Short || valor instanceof Byte) {
                return ((Number) valor).longValue();
            }
            if (valor instanceof BigInteger b) {
                return b.longValueExact();
            }
            if (valor instanceof BigDecimal b) {
                return b.longValueExact();
            }
            if (valor instanceof Double || valor instanceof Float) {
                double d = ((Number) valor).doubleValue();
                return Double.isFinite(d) ? BigDecimal.valueOf(d).longValueExact() : null;
            }
        } catch (ArithmeticException e) {
            return null;
        }
        return null;
    }

    /** Inteiro dentro da faixa de {@code int}, ou {@code null}. */
    public static Integer inteiroPequeno(Object valor) {
        Long numero = inteiro(valor);
        return numero != null && numero >= Integer.MIN_VALUE && numero <= Integer.MAX_VALUE ? numero.intValue() : null;
    }

    /** Data e hora no formato 2026-10-14T15:30:00Z (também aceita deslocamento de fuso), ou {@code null}. */
    public static Instant instante(Object valor) {
        String texto = texto(valor);
        if (texto == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(texto.trim()).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
