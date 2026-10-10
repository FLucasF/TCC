package com.loja.checkout.dominio.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.PedidoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.NivelClubeRegistro;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.CupomRegistro;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntregaRegistro;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamentoRegistro;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.seguro.Seguro;
import com.loja.checkout.infra.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ServicoCheckout {

    public ResumoResponse calcular(PedidoRequest pedido) {
        // 1. Valida itens
        validarItens(pedido.itens());

        // Calcula subtotal e peso total agora que itens são válidos
        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotalKg = calcularPesoTotal(pedido.itens());

        // 2. Valida nível do clube
        NivelClube clube = NivelClubeRegistro.buscar(pedido.nivelClube());

        // 3. Valida região
        Seguro.validarRegiao(pedido.regiao());

        // 4+5. Valida modalidade (existência e disponibilidade)
        ModalidadeEntrega modalidade = ModalidadeEntregaRegistro.buscar(pedido.modalidadeEntrega());
        modalidade.validarDisponibilidade(pesoTotalKg);

        // 6+7. Valida cupom (existência e aplicabilidade), se informado
        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            cupom = CupomRegistro.buscar(pedido.cupom());
            cupom.validarAplicabilidade(subtotalProdutos);
        }

        // 8+9. Valida forma de pagamento (existência e parcelas)
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        FormaPagamento formaPagamento = FormaPagamentoRegistro.buscar(pedido.formaPagamento());
        formaPagamento.validarParcelas(parcelas);

        // Calcula frete: OURO isenta
        BigDecimal frete = clube.isentaFrete()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotalKg);

        // Calcula desconto do cupom
        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, frete, pedido.itens())
                : BigDecimal.ZERO.setScale(2);

        // Calcula seguro (sempre sobre subtotalProdutos, sem desconto e sem frete)
        BigDecimal seguro = Seguro.calcular(pedido.regiao(), subtotalProdutos);

        // Total antes do pagamento
        BigDecimal total = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 10. Valida disponibilidade da forma de pagamento com o total calculado
        formaPagamento.validarDisponibilidade(total);

        // Aplica forma de pagamento
        ResultadoPagamento resultado = formaPagamento.calcular(total, parcelas);

        // Crédito e brinde são calculados sobre subtotalProdutos (sem desconto e sem frete)
        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                resultado.ajustePagamento(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario()
                        .multiply(BigDecimal.valueOf(i.quantidade()))
                        .setScale(2, RoundingMode.HALF_EVEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
