package com.loja.checkout.entrega;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivelPara(PedidoContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContexto contexto) {
        return Dinheiro.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(contexto.pesoTotalKg())));
    }
}
