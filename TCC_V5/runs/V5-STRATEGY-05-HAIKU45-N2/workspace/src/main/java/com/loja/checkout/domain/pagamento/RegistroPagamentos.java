package com.loja.checkout.domain.pagamento;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.loja.checkout.domain.FormaPagamento;

public class RegistroPagamentos {
    private static final Map<FormaPagamento, EstrategiaPagamento> pagamentos = new HashMap<>();

    static {
        pagamentos.put(FormaPagamento.PIX, new PagamentoPix());
        pagamentos.put(FormaPagamento.CARTAO, new PagamentoCartao());
        pagamentos.put(FormaPagamento.BOLETO, new PagamentoBoleto());
    }

    public static Optional<EstrategiaPagamento> obter(FormaPagamento forma) {
        return Optional.ofNullable(pagamentos.get(forma));
    }
}
