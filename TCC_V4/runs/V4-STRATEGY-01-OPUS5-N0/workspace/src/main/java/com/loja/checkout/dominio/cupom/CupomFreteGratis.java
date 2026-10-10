package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente nao paga o frete: no resumo o frete aparece normalmente e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(DadosCupom dados) {
        return Moeda.emCentavos(dados.frete());
    }
}
