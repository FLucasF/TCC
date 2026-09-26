package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ValoresPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements AVista {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Parcelamento calcular(ValoresPedido valores, int parcelas) {
        BigDecimal total = valores.total();
        BigDecimal totalFinal = Dinheiro.centavos(total.subtract(Dinheiro.percentual(total, "5")));
        return new Parcelamento(totalFinal, totalFinal);
    }
}
