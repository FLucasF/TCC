package com.loja.checkout;

import com.loja.checkout.clube.BeneficiosClube;
import com.loja.checkout.cupom.CalculoCupom;
import com.loja.checkout.entrega.CalculoEntrega;
import com.loja.checkout.pagamento.CalculoPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, CalculoEntrega> modalidades;
    private final Map<String, CalculoCupom> cupons;
    private final Map<String, BeneficiosClube> niveis;
    private final Map<String, CalculoPagamento> pagamentos;

    public CheckoutService(List<CalculoEntrega> todasModalidades,
                           List<CalculoCupom> todosCupons,
                           List<BeneficiosClube> todosNiveis,
                           List<CalculoPagamento> todosPagamentos) {
        this.modalidades = todasModalidades.stream()
                .collect(Collectors.toMap(CalculoEntrega::getCodigo, Function.identity()));
        this.cupons = todosCupons.stream()
                .collect(Collectors.toMap(CalculoCupom::getCodigo, Function.identity()));
        this.niveis = todosNiveis.stream()
                .collect(Collectors.toMap(BeneficiosClube::getCodigo, Function.identity()));
        this.pagamentos = todosPagamentos.stream()
                .collect(Collectors.toMap(CalculoPagamento::getCodigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        BeneficiosClube clube = niveis.get(request.nivelClube());
        if (clube == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.porCodigo(request.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        CalculoEntrega entrega = modalidades.get(request.modalidadeEntrega());
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!entrega.isDisponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        CalculoCupom cupom = null;
        BigDecimal subtotal = calcularSubtotal(request.itens());
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            cupom = cupons.get(request.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
            if (!cupom.isAplicavel(subtotal, request.itens())) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        CalculoPagamento pagamento = pagamentos.get(request.formaPagamento());
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        if (!pagamento.isParcelamentoValido(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        if (clube.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, request.itens(), frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = subtotal.multiply(regiao.getTaxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!pagamento.isDisponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.ganhaBrinde(subtotal);

        return new CheckoutResponse(
                subtotal, descontoCupom, frete, entrega.getPrazoDias(),
                seguro, ajuste, resultado.totalFinal(), parcelas,
                resultado.valorParcela(), credito, brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
