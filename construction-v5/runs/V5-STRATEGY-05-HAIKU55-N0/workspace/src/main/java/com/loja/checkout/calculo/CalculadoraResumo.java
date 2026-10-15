package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pedido.Item;
import com.loja.checkout.pedido.Pedido;

@Component
public class CalculadoraResumo {

    private final List<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<FormaPagamento> formasPagamento) {
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(Pedido pedido) {
        List<Item> itens = pedido.itens();

        BigDecimal subtotal = itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = pedido.nivelClube().isFreteGratis()
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : Dinheiro.arredondar(pedido.modalidadeEntrega().frete(pesoTotal));

        BigDecimal desconto = pedido.cupom() == null
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : Dinheiro.arredondar(pedido.cupom().desconto(itens, subtotal, frete));

        BigDecimal seguro = Dinheiro.percentual(subtotal, pedido.regiao().getPercentualSeguro());

        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento formaPagamento = formasPagamento.stream()
                .filter(f -> f.codigo().equals(pedido.formaPagamento()))
                .findFirst()
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        if (!formaPagamento.aceitaParcelas(pedido.parcelas())) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(total)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = formaPagamento.cobrar(total, pedido.parcelas());

        BigDecimal ajuste = cobranca.valorFinal().subtract(total);

        BigDecimal credito = Dinheiro.percentual(subtotal, pedido.nivelClube().getPercentualCredito());

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                pedido.modalidadeEntrega().prazoDias(),
                seguro,
                ajuste,
                cobranca.valorFinal(),
                pedido.parcelas(),
                cobranca.valorParcela(),
                credito,
                pedido.nivelClube().temBrinde(subtotal));
    }
}
