package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.infra.CheckoutException;

import java.util.Map;

public class FormaPagamentoRegistro {

    private static final Map<String, FormaPagamento> REGISTRO = Map.of(
            "PIX", new PagamentoPix(),
            "CARTAO", new PagamentoCartao(),
            "BOLETO", new PagamentoBoleto()
    );

    public static FormaPagamento buscar(String codigo) {
        if (codigo == null || !REGISTRO.containsKey(codigo)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return REGISTRO.get(codigo);
    }
}
