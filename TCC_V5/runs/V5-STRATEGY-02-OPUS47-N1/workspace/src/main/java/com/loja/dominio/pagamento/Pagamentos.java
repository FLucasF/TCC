package com.loja.dominio.pagamento;

import com.loja.dominio.ErroCheckout;

import java.util.Map;
import java.util.function.Supplier;

public final class Pagamentos {
    private static final Map<String, Supplier<FormaPagamento>> REGISTRO = Map.of(
        "PIX", Pix::new,
        "CARTAO", Cartao::new,
        "BOLETO", Boleto::new
    );

    private Pagamentos() {}

    public static FormaPagamento parse(String codigo) {
        if (codigo == null) throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        var sup = REGISTRO.get(codigo);
        if (sup == null) throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        return sup.get();
    }
}
