package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bemvindo10 implements Cupom {

    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), TAXA);
    }
}
