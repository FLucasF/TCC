package br.com.marealta.domain;

import java.util.Map;

/** Documento emitido ao hóspede: dados estruturados mais o texto como sai no e-mail. */
public record Document(String title, ReceiptData data, Map<String, String> identityFields, String text) {
}
