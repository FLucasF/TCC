package com.loja.checkout.servico;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutServico {

    private final ModalidadesEntrega modalidadesEntrega;
    private final Cupons cupons;
    private final FormasPagamento formasPagamento;

    public CheckoutServico(ModalidadesEntrega modalidadesEntrega, Cupons cupons, FormasPagamento formasPagamento) {
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        // 1. Validar pedido
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        for (ResumoRequest.Item item : request.itens()) {
            if (item.nome() == null
                    || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
        }

        // 2. Calcular subtotal e peso total
        BigDecimal subtotalProdutos = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ResumoRequest.Item item : request.itens()) {
            subtotalProdutos = subtotalProdutos.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
            pesoTotal = pesoTotal.add(
                    item.pesoKg().multiply(new BigDecimal(item.quantidade()))
            );
        }
        subtotalProdutos = subtotalProdutos.setScale(2, RoundingMode.HALF_EVEN);

        // 3. Validar nível do clube
        if (request.nivelClube() == null || request.nivelClube().isBlank()) {
            throw new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        }
        NivelClube nivelClube;
        try {
            nivelClube = NivelClube.valueOf(request.nivelClube());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        }

        // 4. Validar região
        if (request.regiao() == null || request.regiao().isBlank()) {
            throw new CheckoutException(ErroCheckout.REGIAO_INVALIDA);
        }
        Regiao regiao;
        try {
            regiao = Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCheckout.REGIAO_INVALIDA);
        }

        // 5. Validar modalidade de entrega
        if (request.modalidadeEntrega() == null || request.modalidadeEntrega().isBlank()) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega entrega = modalidadesEntrega.encontrar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));

        // 6. Verificar disponibilidade da modalidade
        if (!entrega.estaDisponivel(pesoTotal)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        // 7. Calcular frete
        BigDecimal freteBase = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.isFreteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : freteBase;

        // 8 & 9. Processar cupom
        BigDecimal descontoCupom;
        if (request.cupom() == null || request.cupom().isBlank()) {
            descontoCupom = new BigDecimal("0.00");
        } else {
            Cupom cupom = cupons.encontrar(request.cupom())
                    .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
            if (!cupom.eAplicavel(subtotalProdutos)) {
                throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, request.itens());
        }

        // 10. Calcular seguro
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        // 11. Calcular total do pedido
        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 12. Número de parcelas
        int numParcelas = request.parcelas() != null ? request.parcelas() : 1;

        // 13. Validar forma de pagamento
        if (request.formaPagamento() == null || request.formaPagamento().isBlank()) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento pagamento = formasPagamento.encontrar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));

        // 14. Validar parcelamento
        if (!pagamento.isParcelasValidas(numParcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        // 15. Verificar disponibilidade da forma de pagamento
        if (!pagamento.estaDisponivel(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        // 16. Calcular total final e parcela
        BigDecimal totalFinal = pagamento.calcularTotalFinal(totalPedido, numParcelas);
        BigDecimal valorParcela = pagamento.calcularValorParcela(totalFinal, numParcelas);
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        // 17. Crédito e brinde
        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.isDireitoABrinde(subtotalProdutos);

        // 18. Retornar response
        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                numParcelas,
                valorParcela,
                credito,
                brinde
        );
    }
}
