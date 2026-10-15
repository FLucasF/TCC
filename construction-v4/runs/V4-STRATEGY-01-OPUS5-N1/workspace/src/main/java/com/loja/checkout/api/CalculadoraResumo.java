package com.loja.checkout.api;

import com.loja.checkout.dominio.BaseCupom;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Enums;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra, na ordem em que o valor final e calculado. */
@Service
public class CalculadoraResumo {

    private static final int PARCELA_UNICA = 1;

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<Item> itens = itens(pedido.itens());
        NivelClube nivel = Enums.obrigatorio(NivelClube.class, pedido.nivelClube(),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Enums.obrigatorio(Regiao.class, pedido.regiao(),
                CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = Enums.obrigatorio(ModalidadeEntrega.class,
                pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoKg = soma(itens.stream().map(Item::peso).toList());
        if (!modalidade.atende(pesoKg)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.centavos(soma(itens.stream().map(Item::total).toList()));
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pesoKg);
        BigDecimal descontoCupom = descontoCupom(pedido.cupom(),
                new BaseCupom(itens, subtotalProdutos, frete));

        FormaPagamento formaPagamento = Enums.obrigatorio(FormaPagamento.class,
                pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? PARCELA_UNICA : pedido.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.cobrar(totalPedido, parcelas);
        return new ResumoResponse(subtotalProdutos, descontoCupom, frete, modalidade.prazoDias(),
                seguro, pagamento.ajuste(), pagamento.totalFinal(), pagamento.parcelas(),
                pagamento.valorParcela(), nivel.creditoProximaCompra(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private List<Item> itens(List<ItemRequest> enviados) {
        if (enviados == null || enviados.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>(enviados.size());
        for (ItemRequest enviado : enviados) {
            itens.add(item(enviado));
        }
        return List.copyOf(itens);
    }

    private Item item(ItemRequest enviado) {
        if (enviado == null || !positivo(enviado.precoUnitario()) || !positivo(enviado.pesoKg())
                || enviado.quantidade() == null || enviado.quantidade() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(enviado.nome(), enviado.precoUnitario(), enviado.quantidade(),
                enviado.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoCupom(String codigo, BaseCupom base) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = Enums.obrigatorio(Cupom.class, codigo, CodigoErro.CUPOM_INVALIDO);
        if (!cupom.aplicavel(base)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(base);
    }

    private BigDecimal soma(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
