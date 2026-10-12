package com.loja.checkout.service;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.pagamento.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FormaPagamentoFactory {
    private final Map<String, FormaPagamento> formas = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public FormaPagamento obter(String codigo) {
        return formas.get(codigo);
    }

    public boolean existe(String codigo) {
        return formas.containsKey(codigo);
    }
}
