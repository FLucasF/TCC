package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pagamento;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Ordem de calculo do resumo, igual para qualquer compra: cada etapa pergunta
 * a opcao escolhida pelo cliente quanto ela cobra.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(CheckoutRequest compra) {
        Pedido pedido = montarPedido(compra.itens());
        NivelClube nivel = Catalogo.exigir(NivelClube.class, compra.nivelClube(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Catalogo.exigir(Regiao.class, compra.regiao(), ErroCheckout.REGIAO_INVALIDA);

        ModalidadeEntrega modalidade =
                Catalogo.exigir(ModalidadeEntrega.class, compra.modalidadeEntrega(), ErroCheckout.MODALIDADE_INVALIDA);
        exigir(modalidade.atende(pedido), ErroCheckout.MODALIDADE_INDISPONIVEL);

        Optional<Cupom> cupom = cupomInformado(compra.cupom());
        cupom.ifPresent(c -> exigir(c.aplicavel(pedido), ErroCheckout.CUPOM_NAO_APLICAVEL));

        FormaPagamento forma =
                Catalogo.exigir(FormaPagamento.class, compra.formaPagamento(), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = compra.parcelas() == null ? 1 : compra.parcelas();
        exigir(forma.aceitaParcelas(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal desconto = cupom.map(c -> c.desconto(pedido, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete).add(seguro));

        exigir(forma.atende(totalPedido), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = forma.cobrar(totalPedido, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.temBrinde(subtotal));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), ErroCheckout.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::montarItem).toList());
    }

    private Item montarItem(ItemRequest item) {
        exigir(item != null, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), ErroCheckout.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), ErroCheckout.PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Optional<Cupom> cupomInformado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Catalogo.exigir(Cupom.class, codigo, ErroCheckout.CUPOM_INVALIDO));
    }

    private void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}
