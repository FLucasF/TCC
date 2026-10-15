package com.loja.checkout.service.pagamento;

import com.loja.checkout.model.FormaPagamento;

public class FactoryPagamento {
    public static EstrategiaPagamento criar(FormaPagamento forma) {
        return switch (forma) {
            case PIX -> new EstrategiaPagamento.Pix();
            case CARTAO -> new EstrategiaPagamento.Cartao();
            case BOLETO -> new EstrategiaPagamento.Boleto();
        };
    }
}
