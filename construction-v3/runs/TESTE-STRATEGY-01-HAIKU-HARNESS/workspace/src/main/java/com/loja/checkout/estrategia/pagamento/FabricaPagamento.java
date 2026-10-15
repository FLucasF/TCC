package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.modelo.FormaPagamento;

public class FabricaPagamento {
    public static EstrategiaPagamento criar(String formaStr) {
        if (formaStr == null || formaStr.isBlank()) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            FormaPagamento forma = FormaPagamento.valueOf(formaStr);
            return switch (forma) {
                case PIX -> new PagamentoPix();
                case CARTAO -> new PagamentoCartao();
                case BOLETO -> new PagamentoBoleto();
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
