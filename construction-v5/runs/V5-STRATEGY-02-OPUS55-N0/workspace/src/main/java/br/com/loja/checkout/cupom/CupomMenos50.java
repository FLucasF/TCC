package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 50,00 de desconto para compras a partir de R$ 300,00 em produtos. */
@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = Dinheiro.reais("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = Dinheiro.reais("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return carrinho.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(Carrinho carrinho, BigDecimal frete) {
        return DESCONTO;
    }
}
