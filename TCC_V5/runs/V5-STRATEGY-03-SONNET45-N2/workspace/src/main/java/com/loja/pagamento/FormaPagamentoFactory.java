package com.loja.pagamento;

import com.loja.exception.PedidoException;
import java.util.Map;

public class FormaPagamentoFactory {
    private static final Map<String, FormaPagamento> FORMAS = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public static FormaPagamento criar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new PedidoException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento forma = FORMAS.get(codigo);
        if (forma == null) {
            throw new PedidoException("FORMA_PAGAMENTO_INVALIDA");
        }

        return forma;
    }
}
