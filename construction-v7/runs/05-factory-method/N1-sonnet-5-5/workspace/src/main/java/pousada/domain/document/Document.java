package pousada.domain.document;

import java.util.Map;

/** Documento emitido ao hóspede; {@code text} é o texto como sai no e-mail. */
public record Document(String title, Voucher voucher, Map<String, String> identification, String text) {
}
