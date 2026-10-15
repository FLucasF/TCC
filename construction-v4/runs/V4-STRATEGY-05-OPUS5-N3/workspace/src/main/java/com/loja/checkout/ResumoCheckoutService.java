package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadesEntrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra: confere o pedido na ordem definida e monta o
 * resultado pedindo a cada eixo (entrega, cupom, clube, regiao, pagamento) a
 * sua parte da conta.
 */
@Service
public class ResumoCheckoutService {

    private static final int PARCELA_UNICA = 1;

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<ItemPedido> itens = itensDe(pedido);
        NivelClube nivel = NiveisClube.REGISTRO.buscar(pedido.nivelClube())
                .orElseThrow(() -> recusa(ErroCheckout.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> recusa(ErroCheckout.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = ModalidadesEntrega.REGISTRO.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa(ErroCheckout.MODALIDADE_INVALIDA));

        BigDecimal pesoKg = pesoDe(itens);
        if (!modalidade.atende(pesoKg)) {
            throw recusa(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = subtotalDe(itens);
        BigDecimal frete = nivel.frete(modalidade.frete(pesoKg));
        BigDecimal descontoCupom = descontoDe(pedido.cupom(), new ContextoCupom(itens, subtotalProdutos, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.arredonda(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = FormasPagamento.REGISTRO.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusa(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? PARCELA_UNICA : pedido.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw recusa(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw recusa(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredonda(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    /** O carrinho precisa ter itens, e todo item precisa de preco, quantidade e peso. */
    private List<ItemPedido> itensDe(ResumoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw recusa(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ResumoRequest.ItemRequest item : pedido.itens()) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.quantidade())
                    || !positivo(item.pesoKg())) {
                throw recusa(ErroCheckout.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean positivo(Integer valor) {
        return valor != null && valor > 0;
    }

    private BigDecimal subtotalDe(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.total());
        }
        return Dinheiro.arredonda(subtotal);
    }

    /** O peso do pedido nao e arredondado. */
    private BigDecimal pesoDe(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.peso());
        }
        return peso;
    }

    private BigDecimal descontoDe(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = Cupons.REGISTRO.buscar(codigo)
                .orElseThrow(() -> recusa(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw recusa(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private CheckoutException recusa(ErroCheckout erro) {
        return new CheckoutException(erro);
    }
}
