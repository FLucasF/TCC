package com.loja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loja.cupom.Cupom;
import com.loja.cupom.CupomRegistry;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.model.Item;
import com.loja.payment.PaymentMethod;
import com.loja.payment.PaymentMethodRegistry;
import com.loja.shipping.ShippingMethod;
import com.loja.shipping.ShippingMethodRegistry;

@Service
public class CheckoutService {
    private final ShippingMethodRegistry shippingRegistry;
    private final CupomRegistry cupomRegistry;
    private final PaymentMethodRegistry paymentRegistry;

    public CheckoutService(ShippingMethodRegistry shippingRegistry,
                          CupomRegistry cupomRegistry,
                          PaymentMethodRegistry paymentRegistry) {
        this.shippingRegistry = shippingRegistry;
        this.cupomRegistry = cupomRegistry;
        this.paymentRegistry = paymentRegistry;
    }

    public CheckoutResponse calculate(CheckoutRequest request) throws CheckoutException {
        // Validate request
        validateRequest(request);

        List<Item> itens = request.getItens();
        String modalidadeEntrega = request.getModalidadeEntrega();
        String cupomCode = request.getCupom();
        String formaPagamento = request.getFormaPagamento();
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        // 1. Calculate subtotal
        BigDecimal subtotalProdutos = calculateSubtotal(itens);

        // Get shipping method
        ShippingMethod shippingMethod = shippingRegistry.get(modalidadeEntrega);
        if (shippingMethod == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // Check if shipping is available
        BigDecimal totalWeight = calculateTotalWeight(itens);
        if (!shippingMethod.isAvailable(totalWeight)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 2. Calculate discount from cupom
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        if (cupomCode != null && !cupomCode.isEmpty()) {
            Cupom cupom = cupomRegistry.get(cupomCode);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            // Calculate frete first (without rounding for discount calculation purposes)
            BigDecimal frete = shippingMethod.calculateCost(totalWeight)
                    .setScale(2, RoundingMode.HALF_EVEN);

            if (!cupom.isApplicable(itens, subtotalProdutos)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }

            descontoCupom = cupom.calculateDiscount(itens, subtotalProdutos, frete);
        }

        // 3. Calculate frete
        BigDecimal frete = shippingMethod.calculateCost(totalWeight)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 4. Calculate total before payment adjustment
        // total do pedido = produtos − desconto do cupom + frete
        BigDecimal totalBeforePayment = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .setScale(2, RoundingMode.HALF_EVEN);

        // Validate payment method and parcelas
        PaymentMethod paymentMethod = paymentRegistry.get(formaPagamento);
        if (paymentMethod == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!paymentMethod.isValid(parcelas, totalBeforePayment)) {
            if (formaPagamento.equals("BOLETO") && totalBeforePayment.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // 5. Calculate payment adjustment and final total
        BigDecimal ajustePagamento = paymentMethod.calculateAdjustment(totalBeforePayment, parcelas)
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalFinal = totalBeforePayment.add(ajustePagamento)
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal valorParcela = paymentMethod.calculateInstallmentValue(totalBeforePayment, parcelas)
                .setScale(2, RoundingMode.HALF_EVEN);

        // Build response
        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(shippingMethod.getDeliveryDays());
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);

        return response;
    }

    private void validateRequest(CheckoutRequest request) throws CheckoutException {
        // Check if cart is empty
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        // Check each item
        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        // Check if delivery method is informed
        if (request.getModalidadeEntrega() == null || request.getModalidadeEntrega().isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // Check if payment method is informed
        if (request.getFormaPagamento() == null || request.getFormaPagamento().isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calculateSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal itemTotal = item.getPrecoUnitario()
                    .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateTotalWeight(List<Item> itens) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal itemWeight = item.getPesoKg()
                    .multiply(new BigDecimal(item.getQuantidade()));
            totalWeight = totalWeight.add(itemWeight);
        }
        return totalWeight;
    }
}
