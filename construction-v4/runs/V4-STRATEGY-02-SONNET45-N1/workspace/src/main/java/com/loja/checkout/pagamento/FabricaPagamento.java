package com.loja.checkout.pagamento;

import com.loja.checkout.model.enums.FormaPagamento;
import java.util.Map;

public class FabricaPagamento {
    private static final Map<FormaPagamento, Pagamento> PAGAMENTOS = Map.of(
        FormaPagamento.PIX, new PagamentoPix(),
        FormaPagamento.CARTAO, new PagamentoCartao(),
        FormaPagamento.BOLETO, new PagamentoBoleto()
    );

    public static Pagamento criar(FormaPagamento forma) {
        return PAGAMENTOS.get(forma);
    }
}
