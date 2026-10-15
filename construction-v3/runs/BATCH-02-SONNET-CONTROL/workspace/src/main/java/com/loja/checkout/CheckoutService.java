package com.loja.checkout;

import com.loja.checkout.cupom.CupomHandler;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.cupom.DadosPedido;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntregaHandler;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamentoHandler;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public CheckoutService(ModalidadeEntregaRegistry modalidadeEntregaRegistry,
                            CupomRegistry cupomRegistry,
                            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotal(itens));
        BigDecimal pesoPedido = calcularPeso(itens);

        ModalidadeEntregaHandler modalidadeHandler = modalidadeEntregaRegistry.resolver(request.modalidadeEntrega());
        if (!modalidadeHandler.disponivel(pesoPedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = modalidadeHandler.calcularFrete(pesoPedido);

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            CupomHandler cupomHandler = cupomRegistry.resolver(request.cupom());
            DadosPedido dadosPedido = new DadosPedido(itens, subtotalProdutos, frete);
            if (!cupomHandler.aplicavel(dadosPedido)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupomHandler.calcularDesconto(dadosPedido);
        }

        FormaPagamentoHandler formaPagamentoHandler = formaPagamentoRegistry.resolver(request.formaPagamento());

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamentoHandler.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        if (!formaPagamentoHandler.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamentoHandler.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(resultadoPagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidadeHandler.prazoEntregaDias(),
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela()
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
