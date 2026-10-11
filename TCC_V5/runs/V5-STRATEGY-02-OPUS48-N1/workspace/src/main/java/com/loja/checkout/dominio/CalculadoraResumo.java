package com.loja.checkout.dominio;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra. A sequência de validações e a ordem do cálculo são
 * iguais para todo pedido; o que muda de caso para caso mora em cada enum
 * (entrega, cupom, pagamento, clube) e na região.
 */
@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest req) {
        List<ItemPedido> itens = validarItens(req == null ? null : req.itens());
        NivelClube nivel = aceitar(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = aceitar(Regiao.class, req.regiao(), "REGIAO_INVALIDA");

        BigDecimal subtotal = Dinheiro.centavos(somar(itens, ItemPedido::totalLinha));
        BigDecimal peso = somar(itens, ItemPedido::pesoLinha);

        ModalidadeEntrega modalidade = aceitar(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.atende(peso)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = nivel.pagaFrete()
                ? Dinheiro.centavos(modalidade.frete(peso))
                : Dinheiro.centavos(BigDecimal.ZERO);

        BigDecimal descontoCupom = descontoDoCupom(req.cupom(), new ContextoCupom(itens, subtotal, frete));

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento = aceitar(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.valorFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajuste,
                resultado.valorFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.temBrinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validos = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || positivo(item.precoUnitario()) == null
                    || item.quantidade() == null || item.quantidade() <= 0
                    || positivo(item.pesoKg()) == null) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validos.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private BigDecimal descontoDoCupom(String codigo, ContextoCupom ctx) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        Cupom cupom = aceitar(Cupom.class, codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(ctx)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(ctx);
    }

    private static BigDecimal positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0 ? valor : null;
    }

    private static BigDecimal somar(List<ItemPedido> itens, java.util.function.Function<ItemPedido, BigDecimal> parcela) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(parcela.apply(item));
        }
        return total;
    }

    /** Converte o código recebido no caso correspondente; se não existir, recusa com o código de erro dado. */
    private static <E extends Enum<E>> E aceitar(Class<E> tipo, String valor, String erro) {
        if (valor == null) {
            throw new CheckoutException(erro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }
}
