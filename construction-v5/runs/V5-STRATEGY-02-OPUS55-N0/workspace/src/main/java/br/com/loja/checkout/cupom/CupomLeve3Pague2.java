package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal desconto = carrinho.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }
}
