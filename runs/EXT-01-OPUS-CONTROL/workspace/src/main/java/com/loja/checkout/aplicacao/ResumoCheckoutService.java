package com.loja.checkout.aplicacao;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Moeda;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NiveisClube;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.pagamento.ContextoPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra mostrado na finalizacao do pedido. */
@Service
public class ResumoCheckoutService {

    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final NiveisClube niveis;
    private final FormasPagamento formasPagamento;

    public ResumoCheckoutService(ModalidadesEntrega modalidades, Cupons cupons,
                                 NiveisClube niveis, FormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = itensValidados(request);
        NivelClube nivelClube = niveis.porCodigo(codigo(request, ResumoRequest::nivelClube))
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(codigo(request, ResumoRequest::regiao))
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.REGIAO_INVALIDA));

        Pedido pedido = new Pedido(itens, nivelClube, regiao);
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();

        ModalidadeEntrega modalidade = modalidades.porCodigo(codigo(request, ResumoRequest::modalidadeEntrega))
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido.pesoKg())) {
            throw new ErroDeNegocio(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivelClube.temFreteGratis()
                ? Moeda.ZERO
                : modalidade.custo(pedido.pesoKg());

        BigDecimal descontoCupom = descontoCupom(request, itens, subtotalProdutos, frete);

        BigDecimal imposto = regiao.imposto(subtotalProdutos.subtract(descontoCupom));
        BigDecimal totalSemImposto = Moeda.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete));
        BigDecimal totalPedido = Moeda.centavos(totalSemImposto.add(imposto));

        FormaPagamento formaPagamento = formasPagamento.porCodigo(codigo(request, ResumoRequest::formaPagamento))
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.permiteParcelas(parcelas)) {
            throw new ErroDeNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        ContextoPagamento contexto = new ContextoPagamento(totalPedido, totalSemImposto, parcelas);
        if (!formaPagamento.disponivel(contexto)) {
            throw new ErroDeNegocio(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.liquidar(contexto);
        BigDecimal ajustePagamento = Moeda.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                Moeda.centavos(frete),
                modalidade.prazoDias(),
                imposto,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private BigDecimal descontoCupom(ResumoRequest request, List<Item> itens,
                                     BigDecimal subtotalProdutos, BigDecimal frete) {
        String codigoCupom = codigo(request, ResumoRequest::cupom);
        if (codigoCupom == null) {
            return Moeda.ZERO;
        }
        Cupom cupom = cupons.porCodigo(codigoCupom)
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroDeNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private List<Item> itensValidados(ResumoRequest request) {
        if (request == null || request.itens() == null || request.itens().isEmpty()) {
            throw new ErroDeNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        return request.itens().stream().map(this::itemValidado).toList();
    }

    private Item itemValidado(ResumoRequest.ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new ErroDeNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private String codigo(ResumoRequest request,
                          java.util.function.Function<ResumoRequest, String> campo) {
        String valor = campo.apply(request);
        return valor == null || valor.isBlank() ? null : valor;
    }
}
