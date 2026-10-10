package br.com.loja.checkout.calculo;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Pedido ja validado, do carrinho ate o total. Guarda a ordem do calculo, que
 * e a mesma para qualquer entrega, cupom, nivel de clube ou regiao.
 */
public record Pedido(List<Item> itens,
                     ModalidadeEntrega entrega,
                     Optional<Cupom> cupom,
                     NivelClube clube,
                     Regiao regiao) {

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(
                itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal frete() {
        return clube.isentaFrete()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(entrega.frete(pesoKg()));
    }

    public BigDecimal descontoCupom() {
        return cupom.map(c -> Dinheiro.centavos(c.desconto(contextoCupom())))
                .orElse(Dinheiro.ZERO);
    }

    public BigDecimal seguro() {
        return regiao.seguro(subtotalProdutos());
    }

    public BigDecimal totalPedido() {
        return Dinheiro.centavos(subtotalProdutos()
                .subtract(descontoCupom())
                .add(frete())
                .add(seguro()));
    }

    public ContextoCupom contextoCupom() {
        return new ContextoCupom(itens, subtotalProdutos(), frete());
    }
}
