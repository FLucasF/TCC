package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg do pedido, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public BigDecimal calcularFrete(DadosEntrega dados) {
        return Moeda.emCentavos(TAXA_FIXA.add(POR_KG.multiply(dados.pesoKg())));
    }
}
