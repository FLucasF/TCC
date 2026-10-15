package br.com.loja.checkout.servico;

import java.math.BigDecimal;
import java.util.List;

public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        BigDecimal parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequest(String nome, BigDecimal precoUnitario, BigDecimal quantidade, BigDecimal pesoKg) {
    }
}
