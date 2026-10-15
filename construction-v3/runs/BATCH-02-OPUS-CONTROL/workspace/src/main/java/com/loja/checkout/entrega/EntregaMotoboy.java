package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 18,00, no mesmo dia, so para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return VALOR;
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
