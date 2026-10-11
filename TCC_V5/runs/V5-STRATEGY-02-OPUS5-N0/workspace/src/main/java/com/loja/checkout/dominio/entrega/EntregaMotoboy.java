package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Motoboy: R$ 18,00 no mesmo dia, para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements PoliticaEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(BaseCalculo base) {
        return Dinheiro.arredondar(new BigDecimal("18.00"));
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean atende(BaseCalculo base) {
        return base.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
