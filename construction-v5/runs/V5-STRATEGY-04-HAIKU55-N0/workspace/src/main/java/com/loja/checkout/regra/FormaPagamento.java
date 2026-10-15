package com.loja.checkout.regra;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    static FormaPagamento de(String valor) {
        return ErroCheckout.paraEnum(FormaPagamento.class, valor, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }
}
