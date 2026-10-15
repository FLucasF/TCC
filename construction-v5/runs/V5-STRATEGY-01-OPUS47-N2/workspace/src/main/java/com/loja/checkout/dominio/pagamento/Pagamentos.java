package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.ErroPedido;
import java.util.Map;

public final class Pagamentos {
    private static final Map<String, FormaPagamento> TODAS = Map.of(
        "PIX", new Pix(),
        "CARTAO", new Cartao(),
        "BOLETO", new Boleto()
    );

    private Pagamentos() {}

    public static FormaPagamento resolver(String codigo) {
        FormaPagamento f = codigo == null ? null : TODAS.get(codigo);
        if (f == null) throw new ErroPedido("FORMA_PAGAMENTO_INVALIDA");
        return f;
    }
}
