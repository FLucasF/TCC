package br.tcc.checkout.pagamento;

import java.util.HashMap;
import java.util.Map;

public class FormaPagamentoFactory {
    private static final Map<String, FormaPagamento> FORMAS = new HashMap<>();

    static {
        FORMAS.put("PIX", new PixPagamento());
        FORMAS.put("CARTAO", new CartaoPagamento());
        FORMAS.put("BOLETO", new BoletoPagamento());
    }

    public static FormaPagamento criar(String codigo) {
        return FORMAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return FORMAS.containsKey(codigo);
    }
}
