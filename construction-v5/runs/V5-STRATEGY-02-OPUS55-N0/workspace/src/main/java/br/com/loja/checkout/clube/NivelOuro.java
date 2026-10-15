package br.com.loja.checkout.clube;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal PRODUTOS_PARA_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal porcentagemCredito() {
        return new BigDecimal("5");
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(Carrinho carrinho) {
        return carrinho.subtotalProdutos().compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
