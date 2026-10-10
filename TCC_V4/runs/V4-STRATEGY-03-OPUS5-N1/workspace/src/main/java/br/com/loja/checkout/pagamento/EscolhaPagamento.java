package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** A forma de pagamento escolhida e em quantas vezes. */
public record EscolhaPagamento(FormaPagamento forma, int parcelas) {

    public Cobranca cobrar(BigDecimal totalPedido) {
        return forma.cobrar(totalPedido, parcelas);
    }

    public boolean parcelamentoPermitido() {
        return forma.parcelamentoPermitido(parcelas);
    }

    public boolean atende(BigDecimal totalPedido) {
        return forma.atende(totalPedido);
    }
}
