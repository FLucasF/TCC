package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;

import java.util.List;

/** As formas de pagamento aceitas. */
public final class FormasPagamento {

    private static final Catalogo<FormaPagamento> CATALOGO = Catalogo.de(List.of(
            new Pix(),
            new Cartao(),
            new Boleto()
    ), FormaPagamento::codigo);

    private FormasPagamento() {
    }

    public static FormaPagamento exigir(String codigo) {
        return CATALOGO.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(Erro.FORMA_PAGAMENTO_INVALIDA));
    }
}
