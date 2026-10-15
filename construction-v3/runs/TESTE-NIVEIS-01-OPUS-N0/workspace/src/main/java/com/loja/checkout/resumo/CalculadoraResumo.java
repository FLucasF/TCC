package com.loja.checkout.resumo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.CatalogoModalidades;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra na ordem combinada: produtos, cupom, frete,
 * seguro, total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final ValidadorPedido validadorPedido;
    private final CatalogoModalidades modalidades;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumo(ValidadorPedido validadorPedido, CatalogoModalidades modalidades,
            CatalogoCupons cupons, CatalogoFormasPagamento formasPagamento) {
        this.validadorPedido = validadorPedido;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = validadorPedido.validar(solicitacao);
        NivelClube nivelClube = pedido.nivelClube();
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();

        ModalidadeEntrega modalidade = modalidades.buscar(solicitacao.modalidadeEntrega());
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : modalidade.calcularFrete(pedido);

        BigDecimal descontoCupom = calcularDescontoCupom(solicitacao.cupom(), pedido, frete);
        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, pedido.regiao().percentualSeguro());
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(solicitacao.formaPagamento());
        int parcelas = solicitacao.parcelas() == null ? 1 : solicitacao.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(
                pagamento.totalFinal().subtract(totalPedido));

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                Dinheiro.percentual(subtotalProdutos, nivelClube.percentualCredito()),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private BigDecimal calcularDescontoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo);
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }
}
