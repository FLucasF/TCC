package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = BigDecimal.valueOf(0.05);
    private static final BigDecimal MINIMO_BRINDE = BigDecimal.valueOf(500.00);

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
