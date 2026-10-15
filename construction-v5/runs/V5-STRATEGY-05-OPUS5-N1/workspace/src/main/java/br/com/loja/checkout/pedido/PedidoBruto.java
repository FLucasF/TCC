package br.com.loja.checkout.pedido;

import java.util.List;

/** Os dados da compra como o site mandou, ainda sem conferir. */
public record PedidoBruto(
        List<ItemBruto> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public int parcelasOuUma() {
        return parcelas == null ? 1 : parcelas;
    }
}
