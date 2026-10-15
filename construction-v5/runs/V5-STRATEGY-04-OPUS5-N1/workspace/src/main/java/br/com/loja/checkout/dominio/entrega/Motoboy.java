package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = Dinheiro.de("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = Dinheiro.de("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(VALOR);
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
