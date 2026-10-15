package com.loja.checkout.seguro;

import com.loja.checkout.dominio.Regiao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * O seguro usa a mesma fórmula (percentual × subtotal dos produtos) em todas as
 * regiões — só o percentual muda. Por isso não há uma implementação por região, apenas
 * uma tabela (mapa) de percentuais.
 */
@Component
public class SeguroCalculadora {

    private static final Map<Regiao, BigDecimal> PERCENTUAL_POR_REGIAO = Map.of(
            Regiao.SUDESTE, new BigDecimal("0.01"),
            Regiao.SUL, new BigDecimal("0.01"),
            Regiao.CENTRO_OESTE, new BigDecimal("0.015"),
            Regiao.NORTE, new BigDecimal("0.025"),
            Regiao.NORDESTE, new BigDecimal("0.02"));

    public BigDecimal calcularBruto(Regiao regiao, BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_POR_REGIAO.get(regiao));
    }
}
