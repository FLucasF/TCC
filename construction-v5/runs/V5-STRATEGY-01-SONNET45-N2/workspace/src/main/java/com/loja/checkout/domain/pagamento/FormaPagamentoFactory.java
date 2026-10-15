package com.loja.checkout.domain.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import java.util.Map;

public class FormaPagamentoFactory {
    private static final Map<String, FormaPagamento> FORMAS = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public static FormaPagamento criar(String codigo) {
        if (codigo == null || !FORMAS.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return FORMAS.get(codigo);
    }
}
