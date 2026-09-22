package com.loja.checkout.service;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.EntregaService;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.PedidoContexto;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.PagamentoService;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoService {

    private final EntregaService entregaService;
    private final CupomService cupomService;
    private final PagamentoService pagamentoService;

    public ResumoService(EntregaService entregaService, CupomService cupomService, PagamentoService pagamentoService) {
        this.entregaService = entregaService;
        this.cupomService = cupomService;
        this.pagamentoService = pagamentoService;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotal(itens));
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        PedidoContexto pedidoContexto = new PedidoContexto(subtotalProdutos, pesoTotal);
        ModalidadeEntrega modalidade = entregaService.buscar(request.modalidadeEntrega(), pedidoContexto);
        BigDecimal frete = modalidade.calcularFrete(pedidoContexto);

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotalProdutos, itens, frete);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        FormaPagamento formaPagamento = pagamentoService.buscar(request.formaPagamento(), parcelas, totalPedido);
        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                resultadoPagamento.ajuste(),
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

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, BigDecimal subtotalProdutos,
                                              List<ItemRequest> itens, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        CupomContexto cupomContexto = new CupomContexto(subtotalProdutos, itens, frete);
        Cupom cupom = cupomService.buscar(codigoCupom, cupomContexto);
        return Dinheiro.arredondar(cupom.calcularDesconto(cupomContexto));
    }
}
