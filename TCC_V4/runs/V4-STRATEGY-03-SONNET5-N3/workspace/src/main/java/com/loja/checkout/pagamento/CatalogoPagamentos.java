package com.loja.checkout.pagamento;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> formas = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    public FormaPagamento buscar(String codigo) {
        return formas.get(codigo);
    }
}
