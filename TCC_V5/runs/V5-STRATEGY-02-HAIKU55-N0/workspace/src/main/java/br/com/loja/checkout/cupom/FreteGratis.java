package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.ItemCompra;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemCompra> itens, BigDecimal frete) {
        return frete;
    }
}
