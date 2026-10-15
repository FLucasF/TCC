package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal custo(Carrinho carrinho) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(carrinho.pesoTotalKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
