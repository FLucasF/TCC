package br.com.pousadamarealta.reservas.messaging;

/** Uma regra de envio. Cada regra nova é uma classe nova, ligada em {@link MessageRules}. */
public interface MessageRule {

    void apply(Draft draft);
}
