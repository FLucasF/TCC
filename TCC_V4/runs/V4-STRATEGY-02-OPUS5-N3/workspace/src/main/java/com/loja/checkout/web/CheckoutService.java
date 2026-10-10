package com.loja.checkout.web;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.ValorCobrado;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * A parte do cálculo que é igual em todos os casos: confere o pedido na ordem
 * definida, pede a cada opção escolhida o seu pedaço e monta o resumo.
 */
@Service
public class CheckoutService {

    private static final int PARCELA_UNICA = 1;

    public ResumoCompra calcular(ResumoRequest pedido) {
        ResumoRequest dados = Optional.ofNullable(pedido)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.PEDIDO_INVALIDO));

        List<Item> itens = itensValidos(dados.itens());
        NivelClube nivel = opcao(NivelClube.class, dados.nivelClube(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = opcao(Regiao.class, dados.regiao(), ErroCheckout.REGIAO_INVALIDA);
        ModalidadeEntrega entrega =
                opcao(ModalidadeEntrega.class, dados.modalidadeEntrega(), ErroCheckout.MODALIDADE_INVALIDA);

        BigDecimal subtotalProdutos = itens.stream().map(Item::total).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal pesoKg = itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);

        exigir(entrega.atende(pesoKg), ErroCheckout.MODALIDADE_INDISPONIVEL);
        BigDecimal frete = nivel.freteDevido(entrega.frete(pesoKg));

        Optional<Cupom> cupom = cupomInformado(dados.cupom());
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
        cupom.ifPresent(c -> exigir(c.aplicavel(contexto), ErroCheckout.CUPOM_NAO_APLICAVEL));
        BigDecimal descontoCupom = cupom.map(c -> c.desconto(contexto)).orElse(Dinheiro.ZERO);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento pagamento =
                opcao(FormaPagamento.class, dados.formaPagamento(), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Optional.ofNullable(dados.parcelas()).orElse(PARCELA_UNICA);
        exigir(pagamento.permiteParcelas(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);
        exigir(pagamento.atende(totalPedido), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        ValorCobrado cobrado = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                cobrado.totalFinal().subtract(totalPedido),
                cobrado.totalFinal(),
                parcelas,
                cobrado.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private static List<Item> itensValidos(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), ErroCheckout.PEDIDO_INVALIDO);
        return itens.stream().map(CheckoutService::itemValido).toList();
    }

    private static Item itemValido(ItemRequest item) {
        exigir(item != null, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), ErroCheckout.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), ErroCheckout.PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static Optional<Cupom> cupomInformado(String codigo) {
        return Optional.ofNullable(codigo)
                .map(c -> opcao(Cupom.class, c, ErroCheckout.CUPOM_INVALIDO));
    }

    /** Traduz o texto que o site manda na opção correspondente, ou recusa o pedido. */
    private static <O extends Enum<O>> O opcao(Class<O> tipo, String valor, ErroCheckout erro) {
        exigir(valor != null, erro);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException desconhecida) {
            throw new CheckoutException(erro);
        }
    }

    private static void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}
