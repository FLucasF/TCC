package com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class FormasPagamento {
    private final Map<String, FormaPagamento> registro = Map.of(
        "PIX",    new PagamentoPix(),
        "CARTAO", new PagamentoCartao(),
        "BOLETO", new PagamentoBoleto()
    );

    public Optional<FormaPagamento> encontrar(String codigo) {
        return Optional.ofNullable(registro.get(codigo));
    }
}
