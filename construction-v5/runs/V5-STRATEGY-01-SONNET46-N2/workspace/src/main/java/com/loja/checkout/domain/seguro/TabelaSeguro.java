package com.loja.checkout.domain.seguro;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class TabelaSeguro {

    private final Map<String, BigDecimal> taxas;

    public TabelaSeguro() {
        this.taxas = Map.of(
                "SUDESTE",      new BigDecimal("0.010"),
                "SUL",          new BigDecimal("0.010"),
                "CENTRO_OESTE", new BigDecimal("0.015"),
                "NORTE",        new BigDecimal("0.025"),
                "NORDESTE",     new BigDecimal("0.020")
        );
    }

    public BigDecimal taxa(String regiao) {
        return Optional.ofNullable(taxas.get(regiao))
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));
    }

    public boolean regiaoExiste(String regiao) {
        return taxas.containsKey(regiao);
    }
}
