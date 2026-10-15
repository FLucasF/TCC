package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

/**
 * Os dados da compra como o site envia. Tudo chega como pode chegar (ausente,
 * vazio, desconhecido); a conferencia e feita na calculadora, na ordem combinada.
 */
public record RequisicaoResumo(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequisicao(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
