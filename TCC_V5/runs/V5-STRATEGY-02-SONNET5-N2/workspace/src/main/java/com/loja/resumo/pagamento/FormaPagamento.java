package com.loja.resumo.pagamento;

import java.math.BigDecimal;
import java.util.Optional;

public interface FormaPagamento {

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    static Optional<FormaPagamento> encontrar(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(FormaPagamentoPadrao.valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
