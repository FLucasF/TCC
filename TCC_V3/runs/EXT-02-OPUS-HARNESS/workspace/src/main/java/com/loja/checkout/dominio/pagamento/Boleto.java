package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ValoresPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Boleto implements AVista {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean atende(ValoresPedido valores) {
        return valores.totalSemImposto().compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Parcelamento calcular(ValoresPedido valores, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(valores.total().add(TARIFA));
        return new Parcelamento(totalFinal, totalFinal);
    }
}
