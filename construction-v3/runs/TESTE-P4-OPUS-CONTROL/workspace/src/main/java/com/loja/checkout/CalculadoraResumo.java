package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.imposto.Regiao;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.ContextoPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra na ordem combinada: produtos, cupom, frete, imposto,
 * total do pedido e ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumo(CatalogoEntregas entregas,
                             CatalogoCupons cupons,
                             CatalogoNiveisClube niveisClube,
                             CatalogoFormasPagamento formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = lerPedido(request);
        NivelClube nivel = lerNivelClube(request.nivelClube());
        Regiao regiao = lerRegiao(request.regiao());
        ModalidadeEntrega modalidade = lerModalidade(request.modalidadeEntrega(), pedido);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal descontoCupom = lerDesconto(request.cupom(), pedido, subtotalProdutos, frete);

        BigDecimal produtosComDesconto = subtotalProdutos.subtract(descontoCupom);
        BigDecimal imposto = regiao.imposto(produtosComDesconto);
        BigDecimal totalSemImposto = Dinheiro.centavos(produtosComDesconto.add(frete));
        BigDecimal totalPedido = Dinheiro.centavos(totalSemImposto.add(imposto));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        FormaPagamento pagamento = lerFormaPagamento(request.formaPagamento());
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroCheckout(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        ContextoPagamento contexto = new ContextoPagamento(totalPedido, totalSemImposto, parcelas);
        if (!pagamento.disponivel(contexto)) {
            throw new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(contexto);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                imposto,
                Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido)),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private Pedido lerPedido(ResumoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>();
        for (ResumoRequest.ItemRequest item : request.itens()) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itens);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private NivelClube lerNivelClube(String codigo) {
        return niveisClube.buscar(codigo)
                .orElseThrow(() -> new ErroCheckout(CodigoErro.NIVEL_CLUBE_INVALIDO));
    }

    private Regiao lerRegiao(String codigo) {
        return Regiao.buscar(codigo)
                .orElseThrow(() -> new ErroCheckout(CodigoErro.REGIAO_INVALIDA));
    }

    private ModalidadeEntrega lerModalidade(String codigo, Pedido pedido) {
        ModalidadeEntrega modalidade = entregas.buscar(codigo)
                .orElseThrow(() -> new ErroCheckout(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        return modalidade;
    }

    private BigDecimal lerDesconto(String codigo, Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new ErroCheckout(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, subtotal, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroCheckout(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private FormaPagamento lerFormaPagamento(String codigo) {
        return formasPagamento.buscar(codigo)
                .orElseThrow(() -> new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
    }
}
