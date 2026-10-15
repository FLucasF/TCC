package com.loja.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcular(PedidoRequest req) {
        // 1. Validar itens
        validarItens(req.itens());

        // 2. Validar nível do clube
        NivelClube nivel = parseEnum(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Validar região
        Regiao regiao = parseEnum(Regiao.class, req.regiao(), "REGIAO_INVALIDA");

        // 4. Validar modalidade de entrega (existência)
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Validar modalidade de entrega (disponibilidade para o pedido)
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!modalidade.aceita(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = calcularSubtotal(req.itens());

        BigDecimal freteBase = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivel.ajustarFrete(freteBase);

        // 6. Validar cupom (existência)
        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = parseEnum(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
        }

        // 7. Validar cupom (aplicabilidade)
        if (cupom != null) {
            cupom.verificarAplicabilidade(subtotal);
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, req.itens())
                : BigDecimal.ZERO.setScale(2);

        // 8. Validar forma de pagamento (existência)
        FormaPagamento pagamento = parseEnum(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;

        // 9. Validar parcelamento
        pagamento.validarParcelas(parcelas);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 10. Validar forma de pagamento (disponibilidade para o pedido)
        if (!pagamento.disponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        FormaPagamento.Resultado pagResult = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.ganhaBreinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazo(),
                seguro,
                pagResult.ajuste(),
                pagResult.totalFinal(),
                pagResult.parcelas(),
                pagResult.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<PedidoRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (var item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.quantidade() == null || item.quantidade() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<PedidoRequest.Item> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<PedidoRequest.Item> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, String codigoErro) {
        if (value == null) throw new CheckoutException(codigoErro);
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
