package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.CodigoErro;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        long gratis = carrinho.itens().stream().mapToLong(i -> i.quantidade() / 3).sum();
        if (gratis == 0) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return carrinho.itens().stream()
                .map(this::descontoDoItem)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal descontoDoItem(Item item) {
        return Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
    }
}
