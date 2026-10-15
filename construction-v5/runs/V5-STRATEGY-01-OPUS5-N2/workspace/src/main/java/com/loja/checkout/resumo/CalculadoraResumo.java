package com.loja.checkout.resumo;

import com.loja.checkout.resumo.clube.NivelClube;
import com.loja.checkout.resumo.clube.NiveisClube;
import com.loja.checkout.resumo.cupom.BaseCupom;
import com.loja.checkout.resumo.cupom.Cupom;
import com.loja.checkout.resumo.cupom.Cupons;
import com.loja.checkout.resumo.entrega.Entregas;
import com.loja.checkout.resumo.entrega.ModalidadeEntrega;
import com.loja.checkout.resumo.pagamento.FormaPagamento;
import com.loja.checkout.resumo.pagamento.FormasPagamento;
import com.loja.checkout.resumo.pagamento.Parcelamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * A conta do resumo da compra, igual para todos os pedidos: produtos, desconto
 * do cupom, frete, seguro, total do pedido e ajuste da forma de pagamento. O
 * que muda de pedido para pedido fica nas entregas, cupons, níveis do clube,
 * regiões e formas de pagamento.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    public ResumoCompra calcular(PedidoRequest pedido) {
        List<Item> itens = itens(pedido.itens());
        NivelClube nivel = NiveisClube.porCodigo(pedido.nivelClube())
                .orElseThrow(() -> recusa(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> recusa(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega entrega = Entregas.porCodigo(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal pesoKg = pesoTotalKg(itens);
        if (!entrega.atende(pesoKg)) {
            throw recusa(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = subtotalProdutos(itens);
        BigDecimal frete = nivel.frete(entrega.frete(pesoKg));

        Cupom cupom = cupom(pedido.cupom());
        BaseCupom baseCupom = new BaseCupom(itens, subtotalProdutos, frete);
        if (!cupom.aplicavel(baseCupom)) {
            throw recusa(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        BigDecimal descontoCupom = cupom.desconto(baseCupom);

        FormaPagamento pagamento = FormasPagamento.porCodigo(pedido.formaPagamento())
                .orElseThrow(() -> recusa(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELAS_PADRAO);
        if (!pagamento.permiteParcelas(parcelas)) {
            throw recusa(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        if (!pagamento.atende(totalPedido)) {
            throw recusa(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Parcelamento parcelamento = pagamento.calcular(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.centavos(parcelamento.totalFinal().subtract(totalPedido)),
                parcelamento.totalFinal(),
                parcelas,
                parcelamento.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private List<Item> itens(List<ItemRequest> recebidos) {
        if (recebidos == null || recebidos.isEmpty()) {
            throw recusa(CodigoErro.PEDIDO_INVALIDO);
        }
        return recebidos.stream().map(this::item).toList();
    }

    private Item item(ItemRequest recebido) {
        if (recebido == null
                || !positivo(recebido.precoUnitario())
                || !positivo(recebido.pesoKg())
                || recebido.quantidade() == null
                || recebido.quantidade() <= 0) {
            throw recusa(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(recebido.nome(), recebido.precoUnitario(), recebido.quantidade(), recebido.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Cupom cupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Cupom.NENHUM;
        }
        return Cupons.porCodigo(codigo).orElseThrow(() -> recusa(CodigoErro.CUPOM_INVALIDO));
    }

    private BigDecimal subtotalProdutos(List<Item> itens) {
        return Dinheiro.centavos(itens.stream()
                .map(Item::totalProdutos)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** O peso do pedido não é arredondado. */
    private BigDecimal pesoTotalKg(List<Item> itens) {
        return itens.stream()
                .map(Item::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoRecusadoException recusa(CodigoErro codigo) {
        return new PedidoRecusadoException(codigo);
    }
}
