package br.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

/** Dados enviados pelo site. Campos textuais ficam como texto para validarmos com os códigos de erro corretos. */
public record ResumoRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        BigDecimal parcelas,
        String nivelClube,
        String regiao) {

    public record Item(String nome, BigDecimal precoUnitario, BigDecimal quantidade, BigDecimal pesoKg) {
    }
}
