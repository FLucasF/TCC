package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% de credito, nao paga frete nunca e ganha brinde acima de R$ 500,00 em produtos. */
@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal CASHBACK = Dinheiro.de("0.05");
    private static final BigDecimal MINIMO_BRINDE = Dinheiro.de("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CASHBACK);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
