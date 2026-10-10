package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** 5% em credito, nunca paga frete e ganha brinde acima de R$ 500,00 em produtos. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final Dinheiro PRODUTOS_PARA_BRINDE = Dinheiro.de("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public Dinheiro credito(Dinheiro subtotalProdutos) {
        return subtotalProdutos.vezes(TAXA_CREDITO);
    }

    @Override
    public Dinheiro frete(Dinheiro freteDaEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean temBrinde(Dinheiro subtotalProdutos) {
        return subtotalProdutos.maiorQue(PRODUTOS_PARA_BRINDE);
    }
}
