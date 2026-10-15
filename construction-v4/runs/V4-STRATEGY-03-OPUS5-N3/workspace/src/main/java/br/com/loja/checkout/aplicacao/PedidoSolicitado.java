package br.com.loja.checkout.aplicacao;

import java.util.List;

/** A compra como o site envia. Nada aqui e confiavel: tudo passa pela validacao. */
public record PedidoSolicitado(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    private static final int PARCELA_UNICA = 1;

    int parcelasOuUma() {
        return parcelas == null ? PARCELA_UNICA : parcelas;
    }

    boolean temCupom() {
        return cupom != null && !cupom.isBlank();
    }
}
