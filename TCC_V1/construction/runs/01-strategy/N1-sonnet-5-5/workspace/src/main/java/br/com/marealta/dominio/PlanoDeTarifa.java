package br.com.marealta.dominio;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: sabe se a estadia pode usá-lo, como calcula o preço e quanto devolve no cancelamento.
 * Um plano novo é uma subclasse nova, registrada em {@link PlanosDeTarifa}.
 * Por padrão, o plano cobra as noites sem desconto e devolve tudo o que foi pago.
 */
public abstract class PlanoDeTarifa {

    private final String codigo;

    protected PlanoDeTarifa(String codigo) {
        this.codigo = codigo;
    }

    public final String codigo() {
        return codigo;
    }

    /** Lança {@link ApiException#invalida} quando a estadia não pode usar o plano. */
    protected void validar(Estadia estadia) {
    }

    protected BigDecimal diarias(Estadia estadia) {
        return estadia.somaDasNoites();
    }

    protected BigDecimal desconto(Estadia estadia, BigDecimal diarias) {
        return Dinheiro.ZERO;
    }

    public BigDecimal devolucao(BigDecimal valorPago) {
        return valorPago;
    }

    public final Cobranca cobrar(Estadia estadia) {
        validar(estadia);
        BigDecimal diarias = diarias(estadia);
        return new Cobranca(diarias, desconto(estadia, diarias));
    }
}
