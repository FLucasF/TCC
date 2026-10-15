package com.loja.checkout.calculo;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoCompraRequest;
import com.loja.checkout.api.ResumoCompraResponse;
import com.loja.checkout.clube.CatalogoClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra na ordem combinada: produtos, desconto do cupom,
 * frete, seguro, total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class ResumoCompraService {

    private static final int PARCELAS_PADRAO = 1;

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoClube clube;
    private final CatalogoPagamentos pagamentos;

    public ResumoCompraService(CatalogoEntregas entregas, CatalogoCupons cupons,
            CatalogoClube clube, CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.clube = clube;
        this.pagamentos = pagamentos;
    }

    public ResumoCompraResponse calcular(ResumoCompraRequest requisicao) {
        Pedido pedido = montarPedido(requisicao.itens());

        NivelClube nivelClube = clube.porCodigo(requisicao.nivelClube())
                .orElseThrow(() -> recusar(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(requisicao.regiao())
                .orElseThrow(() -> recusar(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = entregas.porCodigo(requisicao.modalidadeEntrega())
                .orElseThrow(() -> recusar(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw recusar(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivelClube.temFreteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal descontoCupom = descontoDoCupom(requisicao.cupom(), pedido, subtotalProdutos, frete);
        BigDecimal seguro = regiao.seguro(subtotalProdutos);

        BigDecimal totalPedido = Dinheiro.arredonda(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = pagamentos.porCodigo(requisicao.formaPagamento())
                .orElseThrow(() -> recusar(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelasPedidas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelasPedidas)) {
            throw recusar(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw recusar(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelasPedidas);
        BigDecimal ajustePagamento = Dinheiro.arredonda(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoCompraResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw recusar(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itensDoPedido = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                    || item.quantidade() == null || item.quantidade() <= 0) {
                throw recusar(CodigoErro.PEDIDO_INVALIDO);
            }
            itensDoPedido.add(new ItemPedido(
                    item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itensDoPedido);
    }

    private BigDecimal descontoDoCupom(String codigo, Pedido pedido,
            BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.porCodigo(codigo)
                .orElseThrow(() -> recusar(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw recusar(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private CheckoutInvalidoException recusar(CodigoErro codigo) {
        return new CheckoutInvalidoException(codigo);
    }
}
