package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.ContextoCupom;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomFactory;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.ModalidadeEntregaFactory;
import com.loja.checkout.domain.pagamento.AjustePagamento;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoFactory;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ResumoService {

    public ResumoResponse calcularResumo(PedidoRequest request) {
        validarPedido(request);

        // 1. Soma dos produtos
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        // 2. Validar nível do clube
        NivelClube nivelClube = validarNivelClube(request.nivelClube());

        // 3. Validar região
        Regiao regiao = validarRegiao(request.regiao());

        // 4. Validar modalidade de entrega
        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.modalidadeEntrega());

        // 5. Verificar se modalidade está disponível
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Calcular frete (pode ser zerado pelo OURO)
        BigDecimal frete = nivelClube.isentarFrete()
            ? BigDecimal.ZERO.setScale(2)
            : modalidade.calcularFrete(pesoTotal);
        int prazo = modalidade.obterPrazo();

        // 7. Validar e aplicar cupom
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = validarCupom(request.cupom());
            ContextoCupom contexto = new ContextoCupom(subtotalProdutos, frete, request.itens());

            if (!cupom.eAplicavel(contexto)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }

            descontoCupom = cupom.calcularDesconto(contexto);
        }

        // 8. Calcular seguro (sempre sobre o subtotal de produtos)
        BigDecimal seguro = Arredondamento.arredondar(
            subtotalProdutos.multiply(regiao.getPercentualSeguro())
        );

        // 9. Total do pedido = produtos - desconto + frete + seguro
        BigDecimal totalPedido = Arredondamento.arredondar(
            subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
        );

        // 10. Validar forma de pagamento
        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());

        // 11. Validar parcelas
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.validarParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // 12. Verificar se forma de pagamento está disponível
        if (!formaPagamento.estaDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // 13. Ajuste da forma de pagamento
        AjustePagamento ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);

        // 14. Benefícios do clube
        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.concederBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazo,
            seguro,
            ajustePagamento.ajuste(),
            ajustePagamento.totalFinal(),
            parcelas,
            ajustePagamento.valorParcela(),
            credito,
            brinde
        );
    }

    private void validarPedido(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal totalItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(totalItem);
        }
        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal pesoItem = item.pesoKg()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidade) {
        if (modalidade == null || modalidade.isBlank()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (!ModalidadeEntregaFactory.existe(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        return ModalidadeEntregaFactory.obter(modalidade);
    }

    private Cupom validarCupom(String codigo) {
        if (!CupomFactory.existe(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        return CupomFactory.obter(codigo);
    }

    private FormaPagamento validarFormaPagamento(String forma) {
        if (forma == null || forma.isBlank()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!FormaPagamentoFactory.existe(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        return FormaPagamentoFactory.obter(forma);
    }
}
