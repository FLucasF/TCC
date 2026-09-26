package com.loja.checkout;

import com.loja.checkout.cupom.ContextoDesconto;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomResolver;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.ResumoCompra;
import com.loja.checkout.entrega.EntregaResolver;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.erro.RegraNegocioException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.PagamentoResolver;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final EntregaResolver entregaResolver;
    private final CupomResolver cupomResolver;
    private final PagamentoResolver pagamentoResolver;

    public CheckoutService(EntregaResolver entregaResolver, CupomResolver cupomResolver,
            PagamentoResolver pagamentoResolver) {
        this.entregaResolver = entregaResolver;
        this.cupomResolver = cupomResolver;
        this.pagamentoResolver = pagamentoResolver;
    }

    public ResumoCompra calcularResumo(CheckoutRequest request) {
        validarPedido(request.itens());

        OpcaoEntrega entrega = entregaResolver.resolver(request.modalidadeEntrega());
        BigDecimal pesoTotal = pesoTotal(request.itens());
        if (!entrega.disponivelPara(pesoTotal)) {
            throw new RegraNegocioException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = entrega.custo(pesoTotal);

        BigDecimal subtotalProdutos = Dinheiro.arredondar(subtotalProdutos(request.itens()));

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null) {
            Cupom cupom = cupomResolver.resolver(request.cupom());
            ContextoDesconto contexto = new ContextoDesconto(request.itens(), subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new RegraNegocioException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(contexto);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = pagamentoResolver.resolver(request.formaPagamento());
        int parcelas = request.parcelasOuPadrao();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new RegraNegocioException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new RegraNegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.totalFinal().subtract(totalPedido);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela()
        );
    }

    private void validarPedido(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new RegraNegocioException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal subtotalProdutos(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::totalPreco).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal pesoTotal(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::totalPeso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
