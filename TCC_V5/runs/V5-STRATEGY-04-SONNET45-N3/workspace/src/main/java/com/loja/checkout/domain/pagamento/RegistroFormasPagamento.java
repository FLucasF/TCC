package com.loja.checkout.domain.pagamento;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;

@Component
public class RegistroFormasPagamento {
    private final Map<String, FormaPagamento> formas = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(formas.get(codigo));
    }
}
