package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveis,
                           List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req.itens());

        NivelClube clube = buscar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscarRegiao(req.regiao());
        ModalidadeEntrega entrega = buscar(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(req.itens());
        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        if (clube.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            ContextoCupom ctx = new ContextoCupom(req.itens(), subtotal, frete);
            if (!cupom.aplicavel(ctx)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(ctx);
        }

        FormaPagamento pagamento = buscar(formasPagamento, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.brinde(subtotal);

        return new ResumoResponse(
                subtotal, descontoCupom, frete, entrega.prazoDias(), seguro,
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela(),
                credito, brinde
        );
    }

    private void validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private Regiao buscarRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T buscar(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T valor = mapa.get(codigo);
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        return valor;
    }
}
