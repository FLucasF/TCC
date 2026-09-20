package br.tcc.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import br.tcc.checkout.domain.Item;
import br.tcc.checkout.util.Arredondador;

public class Leve3Pague2Cupom implements Cupom {
    private List<Item> itens;

    public Leve3Pague2Cupom(List<Item> itens) {
        this.itens = itens;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int quantidadeGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratis));
            desconto = desconto.add(descontoItem);
        }
        return Arredondador.arredondar(desconto);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }
}
