package com.loja.checkout.dominio.entrega;

import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        if (pesoKg.compareTo(PESO_MAXIMO_KG) > 0) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }
}
