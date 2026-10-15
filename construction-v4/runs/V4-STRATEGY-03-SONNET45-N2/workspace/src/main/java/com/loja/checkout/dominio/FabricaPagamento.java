package com.loja.checkout.dominio;

import com.loja.checkout.dominio.pagamento.*;
import java.util.Map;

public class FabricaPagamento {

    private static final Map<FormaPagamento, CalculadoraPagamento> CALCULADORAS = Map.of(
        FormaPagamento.PIX, new PagamentoPix(),
        FormaPagamento.CARTAO, new PagamentoCartao(),
        FormaPagamento.BOLETO, new PagamentoBoleto()
    );

    public CalculadoraPagamento obter(FormaPagamento forma) {
        return CALCULADORAS.get(forma);
    }
}
