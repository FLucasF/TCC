package com.loja.checkout.service;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.BeneficiosClube;
import com.loja.checkout.domain.clube.NiveisClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.Cupons;
import com.loja.checkout.domain.entrega.CalculadoraFrete;
import com.loja.checkout.domain.entrega.Modalidades;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormasPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcular(PedidoRequest req) {
        // 1. Validar itens
        List<ItemRequest> itens = req.itens();
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        BigDecimal subtotalProdutos = arredondar(
            itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        BigDecimal pesoTotal = itens.stream()
            .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Validar nível do clube
        BeneficiosClube clube = NiveisClube.buscar(req.nivelClube());
        if (clube == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        // 3. Validar região
        Regiao regiao = Regiao.buscar(req.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        // 4. Validar modalidade de entrega
        CalculadoraFrete entrega = Modalidades.buscar(req.modalidadeEntrega());
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        // 5. Verificar disponibilidade da modalidade
        if (!entrega.disponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        // 6. Validar cupom (se informado)
        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = Cupons.buscar(req.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        }

        // 7. Verificar aplicabilidade do cupom
        if (cupom != null && !cupom.aplicavel(subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // 8. Validar forma de pagamento
        FormaPagamento forma = FormasPagamento.buscar(req.formaPagamento());
        if (forma == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        // 9. Validar parcelas
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!forma.parcelasValidas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        // Calcular frete (OURO isenta)
        BigDecimal frete = clube.isentoFrete()
            ? arredondar(BigDecimal.ZERO)
            : arredondar(entrega.calcularFrete(pesoTotal));

        // Calcular desconto do cupom
        BigDecimal descontoCupom = cupom != null
            ? arredondar(cupom.calcularDesconto(subtotalProdutos, frete, itens))
            : arredondar(BigDecimal.ZERO);

        // Calcular seguro (sobre subtotalProdutos, sem desconto e sem frete)
        BigDecimal seguro = arredondar(regiao.taxaSeguro.multiply(subtotalProdutos));

        // Total do pedido
        BigDecimal totalPedido = arredondar(
            subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro)
        );

        // 10. Verificar disponibilidade da forma de pagamento
        if (!forma.disponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        // Calcular ajuste de pagamento
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = arredondar(clube.calcularCredito(subtotalProdutos));
        boolean brinde = clube.ganhabrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            entrega.prazoEntregaDias(),
            seguro,
            pagamento.ajuste(),
            pagamento.totalFinal(),
            pagamento.parcelas(),
            pagamento.valorParcela(),
            creditoProximaCompra,
            brinde
        );
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
