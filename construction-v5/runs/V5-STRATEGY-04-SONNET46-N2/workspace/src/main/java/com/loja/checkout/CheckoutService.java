package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NivelClubeRegistro;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistro;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistro;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final NivelClubeRegistro clubeRegistro;
    private final ModalidadeEntregaRegistro entregaRegistro;
    private final CupomRegistro cupomRegistro;
    private final FormaPagamentoRegistro pagamentoRegistro;

    public CheckoutService(NivelClubeRegistro clubeRegistro,
                           ModalidadeEntregaRegistro entregaRegistro,
                           CupomRegistro cupomRegistro,
                           FormaPagamentoRegistro pagamentoRegistro) {
        this.clubeRegistro = clubeRegistro;
        this.entregaRegistro = entregaRegistro;
        this.cupomRegistro = cupomRegistro;
        this.pagamentoRegistro = pagamentoRegistro;
    }

    public ResumoResponse processar(PedidoRequest pedido) {
        List<ItemRequest> itens = pedido.itens();

        // 1. Validar carrinho
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCode.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException(ErroCode.PEDIDO_INVALIDO);
            }
        }

        // 2. Validar nível clube
        if (pedido.nivelClube() == null) {
            throw new CheckoutException(ErroCode.NIVEL_CLUBE_INVALIDO);
        }
        NivelClube clube = clubeRegistro.buscar(pedido.nivelClube());
        if (clube == null) {
            throw new CheckoutException(ErroCode.NIVEL_CLUBE_INVALIDO);
        }

        // 3. Validar região
        if (pedido.regiao() == null) {
            throw new CheckoutException(ErroCode.REGIAO_INVALIDA);
        }
        Regiao regiao;
        try {
            regiao = Regiao.valueOf(pedido.regiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCode.REGIAO_INVALIDA);
        }

        // 4. Validar modalidade entrega
        if (pedido.modalidadeEntrega() == null) {
            throw new CheckoutException(ErroCode.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega modalidade = entregaRegistro.buscar(pedido.modalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException(ErroCode.MODALIDADE_INVALIDA);
        }

        // 5. Calcular peso e verificar disponibilidade da modalidade
        double pesoTotal = itens.stream()
                .mapToDouble(i -> i.pesoKg() * i.quantidade())
                .sum();
        modalidade.verificarDisponibilidade(pesoTotal);

        // 6. Validar cupom
        Cupom cupom = null;
        if (pedido.cupom() != null) {
            cupom = cupomRegistro.buscar(pedido.cupom())
                    .orElseThrow(() -> new CheckoutException(ErroCode.CUPOM_INVALIDO));
        }

        // 7. Validar forma de pagamento
        if (pedido.formaPagamento() == null) {
            throw new CheckoutException(ErroCode.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento formaPagamento = pagamentoRegistro.buscar(pedido.formaPagamento());
        if (formaPagamento == null) {
            throw new CheckoutException(ErroCode.FORMA_PAGAMENTO_INVALIDA);
        }

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        // Pipeline de cálculo
        BigDecimal subtotalProdutos = itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())).setScale(2, RoundingMode.HALF_EVEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal freteBase = modalidade.calcularFrete(pesoTotal);
        BigDecimal freteAjustado = clube.ajustarFrete(freteBase);

        // Verificar aplicabilidade do cupom após calcular frete
        if (cupom != null) {
            cupom.verificarAplicabilidade(subtotalProdutos, freteAjustado);
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, freteAjustado, itens)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(freteAjustado)
                .add(seguro);

        // Verificar parcelamento e disponibilidade da forma de pagamento
        formaPagamento.verificarParcelamento(parcelas);
        formaPagamento.verificarDisponibilidade(totalPedido, parcelas);

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos.setScale(2, RoundingMode.HALF_EVEN),
                descontoCupom.setScale(2, RoundingMode.HALF_EVEN),
                freteAjustado.setScale(2, RoundingMode.HALF_EVEN),
                modalidade.getPrazoEntregaDias(),
                seguro.setScale(2, RoundingMode.HALF_EVEN),
                resultado.ajustePagamento().setScale(2, RoundingMode.HALF_EVEN),
                resultado.totalFinal().setScale(2, RoundingMode.HALF_EVEN),
                parcelas,
                resultado.valorParcela().setScale(2, RoundingMode.HALF_EVEN),
                credito.setScale(2, RoundingMode.HALF_EVEN),
                brinde
        );
    }
}
