package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

        boolean valido() {
            return precoUnitario != null && precoUnitario.signum() > 0
                    && quantidade != null && quantidade > 0
                    && pesoKg != null && pesoKg.signum() > 0;
        }
    }

    public Pedido paraPedido() {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(i -> i == null || !i.valido())) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    public int parcelasOuPadrao() {
        return parcelas == null ? 1 : parcelas;
    }
}
