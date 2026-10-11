package com.loja.checkout.pagamento;

import com.loja.checkout.ErroPedido;

import java.util.Map;

public class RegistroPagamentos {
    private static final Map<String, FormaPagamento> PAGAMENTOS = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    public FormaPagamento obter(String nome) {
        if (nome == null) throw new ErroPedido("FORMA_PAGAMENTO_INVALIDA");
        FormaPagamento p = PAGAMENTOS.get(nome);
        if (p == null) throw new ErroPedido("FORMA_PAGAMENTO_INVALIDA");
        return p;
    }
}
