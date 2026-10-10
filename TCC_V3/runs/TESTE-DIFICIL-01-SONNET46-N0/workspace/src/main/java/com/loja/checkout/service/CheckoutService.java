package com.loja.checkout.service;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.BeneficioClube;
import com.loja.checkout.domain.cupom.AplicadorCupom;
import com.loja.checkout.domain.entrega.OpcaoEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, OpcaoEntrega> entregas;
    private final Map<String, AplicadorCupom> cupons;
    private final Map<String, BeneficioClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public CheckoutService(
            List<OpcaoEntrega> entregas,
            List<AplicadorCupom> cupons,
            List<BeneficioClube> niveis,
            List<FormaPagamento> pagamentos) {
        this.entregas = entregas.stream().collect(Collectors.toMap(OpcaoEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(AplicadorCupom::getCodigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(BeneficioClube::getCodigo, Function.identity()));
        this.pagamentos = pagamentos.stream().collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Validar carrinho
        validarCarrinho(req.itens());

        // 2. Validar nível do clube
        BeneficioClube nivel = buscarOuErro(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Validar região
        Regiao regiao = Regiao.buscar(req.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        // 4. Validar modalidade de entrega
        OpcaoEntrega entrega = buscarOuErro(entregas, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Calcular peso total e checar disponibilidade
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!entrega.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Calcular subtotal (necessário para validação do cupom)
        BigDecimal subtotal = calcularSubtotal(req.itens());

        // 6. Validar cupom (se informado)
        AplicadorCupom cupom = null;
        if (req.cupom() != null) {
            cupom = buscarOuErro(cupons, req.cupom(), "CUPOM_INVALIDO");

            // 7. Checar aplicabilidade do cupom
            if (!cupom.isAplicavel(subtotal, req.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Validar forma de pagamento
        FormaPagamento pagamento = buscarOuErro(pagamentos, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        // 9. Validar número de parcelas
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.isParcelasValida(parcelas, nivel)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // --- Cálculo ---

        // Frete (calculado antes do cupom pois FRETEGRATIS depende dele)
        BigDecimal freteBase = entrega.calcularFrete(pesoTotal).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal frete = nivel.isFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        // Desconto do cupom
        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, req.itens())
                : BigDecimal.ZERO.setScale(2);

        // Seguro (sobre o subtotal de produtos, sem desconto e sem frete)
        BigDecimal seguro = BigDecimal.ZERO.setScale(2);
        if (entrega.temSeguro()) {
            seguro = subtotal.multiply(regiao.getTaxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);
        }

        // Total do pedido = subtotal − desconto + frete + seguro
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 10. Checar disponibilidade da forma de pagamento
        if (!pagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Calcular ajuste de pagamento
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas, nivel);

        // Prazo de entrega
        int prazo = entrega.getPrazoDias() + resultado.diasAdicionais();

        // Crédito e brinde (calculados sobre o subtotal de produtos)
        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                prazo,
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private void validarCarrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
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
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private <T> T buscarOuErro(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || !mapa.containsKey(codigo)) {
            throw new CheckoutException(codigoErro);
        }
        return mapa.get(codigo);
    }
}
