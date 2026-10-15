package com.loja.checkout.service;

import com.loja.checkout.cupom.CupomEstrategia;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.EntregaService;
import com.loja.checkout.entrega.ModalidadeEntregaEstrategia;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.pagamento.PagamentoEstrategia;
import com.loja.checkout.pagamento.PagamentoService;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    private final EntregaService entregaService;
    private final CupomService cupomService;
    private final PagamentoService pagamentoService;

    public CheckoutService(EntregaService entregaService, CupomService cupomService,
                            PagamentoService pagamentoService) {
        this.entregaService = entregaService;
        this.cupomService = cupomService;
        this.pagamentoService = pagamentoService;
    }

    public ResumoResponse calcularResumo(CheckoutRequest request) {
        List<ItemPedido> itens = validarEConverterItens(request.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarTotalItens(itens));
        BigDecimal pesoTotalKg = somarPesoItens(itens);

        ModalidadeEntregaEstrategia entrega = entregaService.buscarEstrategia(request.modalidadeEntrega());
        if (!entrega.disponivel(pesoTotalKg)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.arredondar(entrega.calcularFrete(pesoTotalKg));
        int prazoEntregaDias = entrega.prazoEntregaDias();

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (request.cupom() != null) {
            CupomEstrategia cupom = cupomService.buscarEstrategia(request.cupom());
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(itens, subtotalProdutos, frete));
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        PagamentoEstrategia pagamento = pagamentoService.buscarEstrategia(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela()
        );
    }

    private List<ItemPedido> validarEConverterItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : itensRequest) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private BigDecimal somarTotalItens(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.totalItem());
        }
        return total;
    }

    private BigDecimal somarPesoItens(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoTotalItem());
        }
        return peso;
    }
}
