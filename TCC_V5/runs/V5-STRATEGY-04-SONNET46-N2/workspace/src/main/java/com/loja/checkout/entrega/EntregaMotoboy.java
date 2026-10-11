package com.loja.checkout.entrega;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.ErroCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return new BigDecimal("18.00").setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public void verificarDisponibilidade(double pesoKg) {
        if (pesoKg > 5.0) {
            throw new CheckoutException(ErroCode.MODALIDADE_INDISPONIVEL);
        }
    }
}
