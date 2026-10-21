package br.com.marealta.plan;

/** A reserva não pode ser feita por causa das regras do plano de tarifa. */
public class PlanRuleException extends RuntimeException {
    public PlanRuleException(String message) { super(message); }
}
