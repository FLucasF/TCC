package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.ItemCarrinho;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {
    public String codigo() { return "LEVE3PAGUE2"; }

    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        BigDecimal total = pedido.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(total);
    }
}
