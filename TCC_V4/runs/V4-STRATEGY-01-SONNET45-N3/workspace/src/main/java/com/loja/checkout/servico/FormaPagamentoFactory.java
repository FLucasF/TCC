package com.loja.checkout.servico;

import com.loja.checkout.dominio.pagamento.*;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class FormaPagamentoFactory {

    private final Map<String, FormaPagamento> formas = new HashMap<>();

    public FormaPagamentoFactory() {
        formas.put("PIX", new Pix());
        formas.put("BOLETO", new Boleto());
        formas.put("CARTAO", new Cartao());
    }

    public FormaPagamento obter(String codigo) {
        return formas.get(codigo);
    }
}
