package com.loja.checkout.service;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.RegistroNiveis;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.RegistroEntregas;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.RegistroPagamentos;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.seguro.TabelaSeguro;
import com.loja.checkout.web.dto.CheckoutRequest;
import com.loja.checkout.web.dto.CheckoutResponse;
import com.loja.checkout.web.dto.ItemDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final RegistroEntregas registroEntregas;
    private final RegistroCupons registroCupons;
    private final RegistroNiveis registroNiveis;
    private final RegistroPagamentos registroPagamentos;
    private final TabelaSeguro tabelaSeguro;

    public CheckoutService(RegistroEntregas registroEntregas,
                           RegistroCupons registroCupons,
                           RegistroNiveis registroNiveis,
                           RegistroPagamentos registroPagamentos,
                           TabelaSeguro tabelaSeguro) {
        this.registroEntregas = registroEntregas;
        this.registroCupons = registroCupons;
        this.registroNiveis = registroNiveis;
        this.registroPagamentos = registroPagamentos;
        this.tabelaSeguro = tabelaSeguro;
    }

    public CheckoutResponse processar(CheckoutRequest req) {
        // 1. Validação: pedido
        validarItens(req.itens());

        // 2. Validação: nível clube
        if (req.nivelClube() == null || req.nivelClube().isBlank()) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        NivelClube clube = registroNiveis.buscar(req.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        // 3. Validação: região
        if (req.regiao() == null || req.regiao().isBlank()) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        if (!tabelaSeguro.regiaoExiste(req.regiao())) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        // 4. Validação: modalidade de entrega
        if (req.modalidadeEntrega() == null || req.modalidadeEntrega().isBlank()) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega modalidade = registroEntregas.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        // peso total sem arredondamento
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());

        // 5. Validação: disponibilidade da modalidade
        modalidade.validarDisponibilidade(pesoTotal);

        // 6. Validação: cupom
        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = registroCupons.buscar(req.cupom())
                    .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        }

        // 7. Validação: forma de pagamento
        if (req.formaPagamento() == null || req.formaPagamento().isBlank()) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento formaPagamento = registroPagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = (req.parcelas() == null) ? 1 : req.parcelas();

        // --- CÁLCULO ---

        // Passo 1: subtotalProdutos
        BigDecimal subtotal = calcularSubtotal(req.itens());

        // Passo 3: frete (calculado antes do cupom pois FRETEGRATIS precisa do valor)
        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotal);
        // Se clube OURO → frete = 0
        BigDecimal frete = clube.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : freteCalculado;

        // Passo 2: descontoCupom
        BigDecimal descontoCupom;
        if (cupom != null) {
            cupom.validarAplicabilidade(subtotal, frete, req.itens());
            descontoCupom = cupom.calcularDesconto(subtotal, frete, req.itens());
        } else {
            descontoCupom = BigDecimal.ZERO.setScale(2);
        }

        // Passo 4: seguro = taxa × subtotalProdutos
        BigDecimal taxaSeguro = tabelaSeguro.taxa(req.regiao());
        BigDecimal seguro = subtotal.multiply(taxaSeguro).setScale(2, RoundingMode.HALF_EVEN);

        // Passo 5: totalPedido = subtotal − desconto + frete + seguro
        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // Passo 6: validação e ajuste do pagamento
        formaPagamento.validar(parcelas, totalPedido);
        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        // Clube: crédito e brinde calculados sobre subtotalProdutos
        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.brinde(subtotal);

        return new CheckoutResponse(
                subtotal,
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

    private void validarItens(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemDto> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
