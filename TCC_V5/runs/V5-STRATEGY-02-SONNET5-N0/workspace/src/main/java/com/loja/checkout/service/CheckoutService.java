package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.clube.NivelClubeBeneficio;
import com.loja.checkout.service.cupom.CupomContexto;
import com.loja.checkout.service.cupom.CupomStrategy;
import com.loja.checkout.service.entrega.EntregaCalculadora;
import com.loja.checkout.service.pagamento.FormaPagamentoCalculo;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, EntregaCalculadora> entregasPorCodigo;
    private final Map<String, CupomStrategy> cuponsPorCodigo;
    private final Map<String, NivelClubeBeneficio> niveisPorCodigo;
    private final Map<String, FormaPagamentoCalculo> pagamentosPorCodigo;

    public CheckoutService(List<EntregaCalculadora> entregas,
                            List<CupomStrategy> cupons,
                            List<NivelClubeBeneficio> niveis,
                            List<FormaPagamentoCalculo> pagamentos) {
        this.entregasPorCodigo = entregas.stream()
                .collect(Collectors.toMap(EntregaCalculadora::getCodigo, e -> e));
        this.cuponsPorCodigo = cupons.stream()
                .collect(Collectors.toMap(CupomStrategy::getCodigo, c -> c));
        this.niveisPorCodigo = niveis.stream()
                .collect(Collectors.toMap(NivelClubeBeneficio::getCodigo, n -> n));
        this.pagamentosPorCodigo = pagamentos.stream()
                .collect(Collectors.toMap(FormaPagamentoCalculo::getCodigo, p -> p));
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarItens(request.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(request.itens()));
        BigDecimal pesoTotalKg = somarPeso(request.itens());

        NivelClubeBeneficio beneficioClube = niveisPorCodigo.get(request.nivelClube());
        if (beneficioClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = resolverRegiao(request.regiao());

        EntregaCalculadora entrega = entregasPorCodigo.get(request.modalidadeEntrega());
        if (entrega == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        if (!entrega.disponivel(pesoTotalKg, subtotalProdutos)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteBase = entrega.calcularFrete(pesoTotalKg);
        BigDecimal frete = beneficioClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        String codigoCupom = request.cupom();
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (codigoCupom != null && !codigoCupom.isBlank()) {
            CupomStrategy cupom = cuponsPorCodigo.get(codigoCupom);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            CupomContexto contexto = new CupomContexto(request.itens(), subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(contexto);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamentoCalculo pagamento = pagamentosPorCodigo.get(request.formaPagamento());
        if (pagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = beneficioClube.calcularCredito(subtotalProdutos);
        boolean brinde = beneficioClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.getPrazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal somarSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal somarPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private Regiao resolverRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }
}
