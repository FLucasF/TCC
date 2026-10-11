package com.loja.checkout.aplicacao;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.clube.CatalogoClube;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.CatalogoEntrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.CatalogoPagamentos;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.seguro.Regiao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra que o site mostra antes de confirmar o pedido. */
@Service
public class CalculadoraResumo {

    private final CatalogoEntrega entregas;
    private final CatalogoCupons cupons;
    private final CatalogoClube clube;
    private final CatalogoPagamentos pagamentos;

    public CalculadoraResumo(CatalogoEntrega entregas, CatalogoCupons cupons,
                             CatalogoClube clube, CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.clube = clube;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<ItemPedido> itens = validarItens(pedido);

        NivelClube nivelClube = clube.exigir(
                pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);

        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = entregas.exigir(
                pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoPedido = pesoPedido(itens);
        if (!modalidade.atende(pesoPedido)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = subtotalProdutos(itens);
        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : modalidade.frete(pesoPedido);

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            Cupom cupom = cupons.exigir(pedido.cupom(), CodigoErro.CUPOM_INVALIDO);
            ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = pagamentos.exigir(
                pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);

        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(
                pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private List<ItemPedido> validarItens(ResumoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : pedido.itens()) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(),
                    item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Soma do preco de cada item x quantidade. */
    private BigDecimal subtotalProdutos(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.total());
        }
        return Dinheiro.centavos(subtotal);
    }

    /** Soma do peso de cada item x quantidade, sem arredondar. */
    private BigDecimal pesoPedido(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.peso());
        }
        return peso;
    }
}
