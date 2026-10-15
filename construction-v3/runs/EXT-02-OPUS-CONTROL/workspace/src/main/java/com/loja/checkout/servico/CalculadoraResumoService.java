package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.domain.clube.CatalogoNiveisClube;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.CatalogoCupons;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.CatalogoModalidades;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.erro.CodigoErro;
import com.loja.checkout.domain.erro.RegraNegocioException;
import com.loja.checkout.domain.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.ItemPedido;
import com.loja.checkout.domain.pedido.Pedido;
import com.loja.checkout.domain.regiao.Regiao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem combinada com o financeiro. */
@Service
public class CalculadoraResumoService {

    private final CatalogoModalidades modalidades;
    private final CatalogoCupons cupons;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumoService(CatalogoModalidades modalidades,
                                    CatalogoCupons cupons,
                                    CatalogoNiveisClube niveisClube,
                                    CatalogoFormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = montarPedido(requisicao);
        NivelClube nivel = niveisClube.porCodigo(codigo(requisicao.nivelClube()))
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(codigo(requisicao.regiao()))
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.porCodigo(codigo(requisicao.modalidadeEntrega()))
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new RegraNegocioException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // O frete sai zerado quando o nivel do clube nao cobra frete (OURO).
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);

        BigDecimal descontoCupom = Dinheiro.ZERO;
        String codigoCupom = codigo(requisicao.cupom());
        if (codigoCupom != null) {
            Cupom cupom = cupons.porCodigo(codigoCupom)
                    .orElseThrow(() -> new RegraNegocioException(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavel(pedido, frete)) {
                throw new RegraNegocioException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.desconto(pedido, frete);
        }

        FormaPagamento formaPagamento = formasPagamento.porCodigo(codigo(requisicao.formaPagamento()))
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new RegraNegocioException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal produtosComDesconto = Dinheiro.valor(subtotalProdutos.subtract(descontoCupom));
        BigDecimal imposto = regiao.imposto(produtosComDesconto);
        BigDecimal totalSemImposto = Dinheiro.valor(produtosComDesconto.add(frete));
        BigDecimal totalPedido = Dinheiro.valor(totalSemImposto.add(imposto));

        if (!formaPagamento.atende(totalSemImposto)) {
            throw new RegraNegocioException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.aplicar(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.valor(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(pedido),
                imposto,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(pedido),
                nivel.temBrinde(pedido));
    }

    private Pedido montarPedido(ResumoRequest requisicao) {
        List<ItemRequest> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itensPedido = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new RegraNegocioException(CodigoErro.PEDIDO_INVALIDO);
            }
            itensPedido.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itensPedido);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Texto ausente ou em branco conta como nao informado. */
    private String codigo(String bruto) {
        return bruto == null || bruto.isBlank() ? null : bruto.trim();
    }
}
