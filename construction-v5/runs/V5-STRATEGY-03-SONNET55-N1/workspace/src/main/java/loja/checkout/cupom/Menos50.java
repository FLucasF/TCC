package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.CodigoErro;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        if (carrinho.subtotal().compareTo(MINIMO) < 0) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(new BigDecimal("50"));
    }
}
