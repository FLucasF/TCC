package br.tcc.checkout.dominio.pagamento;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioFormasPagamento {
    private static final Map<String, FormaPagamento> FORMAS = new HashMap<>();

    static {
        FORMAS.put("PIX", new FormaPagamentoPix());
        FORMAS.put("CARTAO", new FormaPagamentoCartao());
        FORMAS.put("BOLETO", new FormaPagamentoBoleto());
    }

    public Optional<FormaPagamento> obter(String codigo) {
        return Optional.ofNullable(FORMAS.get(codigo));
    }

    public static RepositorioFormasPagamento criar() {
        return new RepositorioFormasPagamento();
    }
}
