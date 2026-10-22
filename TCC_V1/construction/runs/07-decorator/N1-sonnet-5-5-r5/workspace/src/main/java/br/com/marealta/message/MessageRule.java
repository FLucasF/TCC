package br.com.marealta.message;

public interface MessageRule {

    /** Nome usado na configuração da instalação. */
    String name();

    void apply(Delivery delivery);
}
