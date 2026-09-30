package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.dominio.BaseCupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Codigos;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Frete;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.loja.checkout.dominio.ErroCheckout.CUPOM_INVALIDO;
import static com.loja.checkout.dominio.ErroCheckout.CUPOM_NAO_APLICAVEL;
import static com.loja.checkout.dominio.ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.checkout.dominio.ErroCheckout.FORMA_PAGAMENTO_INVALIDA;
import static com.loja.checkout.dominio.ErroCheckout.MODALIDADE_INDISPONIVEL;
import static com.loja.checkout.dominio.ErroCheckout.MODALIDADE_INVALIDA;
import static com.loja.checkout.dominio.ErroCheckout.NIVEL_CLUBE_INVALIDO;
import static com.loja.checkout.dominio.ErroCheckout.PEDIDO_INVALIDO;
import static com.loja.checkout.dominio.ErroCheckout.REGIAO_INVALIDA;

/**
 * A sequencia do resumo e sempre a mesma: produtos, cupom, frete, imposto,
 * total do pedido e ajuste do pagamento. O que muda de pedido para pedido fica
 * dentro da modalidade, do cupom, do nivel do clube, da regiao e da forma de
 * pagamento escolhidos.
 */
@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(ResumoRequest pedido) {
        ResumoRequest dados = Optional.ofNullable(pedido)
                .orElseThrow(() -> new CheckoutInvalidoException(PEDIDO_INVALIDO));

        Carrinho carrinho = carrinhoDe(dados.itens());
        NivelClube nivel = Codigos.obrigatorio(NivelClube.class, dados.nivelClube(), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Codigos.obrigatorio(Regiao.class, dados.regiao(), REGIAO_INVALIDA);
        ModalidadeEntrega modalidade =
                Codigos.obrigatorio(ModalidadeEntrega.class, dados.modalidadeEntrega(), MODALIDADE_INVALIDA);
        exigir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = carrinho.subtotal();
        Frete cotacao = modalidade.cotar(carrinho);
        Frete frete = nivel.freteGratis() ? cotacao.gratis() : cotacao;

        Optional<Cupom> cupom = Codigos.opcional(Cupom.class, dados.cupom(), CUPOM_INVALIDO);
        BaseCupom baseCupom = new BaseCupom(carrinho, subtotal, frete.valor());
        exigir(cupom.map(c -> c.aplicavel(baseCupom)).orElse(true), CUPOM_NAO_APLICAVEL);
        BigDecimal descontoCupom = cupom.map(c -> c.abatimento(baseCupom)).orElse(Dinheiro.ZERO);

        FormaPagamento pagamento =
                Codigos.obrigatorio(FormaPagamento.class, dados.formaPagamento(), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Optional.ofNullable(dados.parcelas()).orElse(1);
        exigir(pagamento.permite(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);

        BigDecimal produtosComDesconto = Dinheiro.centavos(subtotal.subtract(descontoCupom));
        BigDecimal totalSemImposto = Dinheiro.centavos(produtosComDesconto.add(frete.valor()));
        exigir(pagamento.disponivel(totalSemImposto), FORMA_PAGAMENTO_INDISPONIVEL);

        BigDecimal imposto = regiao.imposto(produtosComDesconto);
        BigDecimal totalPedido = Dinheiro.centavos(totalSemImposto.add(imposto));
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotal,
                descontoCupom,
                frete.valor(),
                frete.prazoDias(),
                imposto,
                cobranca.ajuste(totalPedido),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private Carrinho carrinhoDe(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), PEDIDO_INVALIDO);
        return new Carrinho(itens.stream().map(this::itemDe).toList());
    }

    private Item itemDe(ItemRequest item) {
        exigir(item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg()), PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw new CheckoutInvalidoException(erro);
        }
    }
}
