package br.com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/**
 * O cliente nao paga o frete: o frete aparece normalmente no resumo e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public Dinheiro desconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
