package com.loja.checkout.service;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidadesEntrega;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(Map<String, ModalidadeEntrega> modalidadesEntrega,
                            Map<String, Cupom> cupons,
                            Map<String, FormaPagamento> formasPagamento) {
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarItens(request.itens());
        List<ItemRequest> itens = request.itens();

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotal(itens));

        ModalidadeEntrega modalidade = modalidadesEntrega.get(request.modalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        BigDecimal pesoTotalKg = calcularPesoTotal(itens);
        modalidade.validarDisponibilidade(pesoTotalKg);
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pesoTotalKg));
        int prazoEntregaDias = modalidade.prazoEntregaDias();

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        String codigoCupom = request.cupom();
        if (codigoCupom != null && !codigoCupom.isBlank()) {
            Cupom cupom = cupons.get(codigoCupom);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            cupom.validarAplicabilidade(subtotalProdutos, itens);
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(subtotalProdutos, frete, itens));
        }

        FormaPagamento formaPagamento = formasPagamento.get(request.formaPagamento());
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        formaPagamento.validarParcelas(parcelas);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        formaPagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela()
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item == null || !item.valido()) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }
}
