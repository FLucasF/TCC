package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PRODUTOS_PARA_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("5");
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
