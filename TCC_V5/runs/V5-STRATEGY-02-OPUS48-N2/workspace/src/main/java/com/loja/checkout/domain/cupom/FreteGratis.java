package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * O cliente nao paga o frete: o frete aparece normalmente no resumo e o desconto
 * fica igual ao valor do frete.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public void validarAplicabilidade(DadosCupom dados) {
        // Sem condicao.
    }

    @Override
    public BigDecimal desconto(DadosCupom dados) {
        return Dinheiro.centavos(dados.frete());
    }
}
