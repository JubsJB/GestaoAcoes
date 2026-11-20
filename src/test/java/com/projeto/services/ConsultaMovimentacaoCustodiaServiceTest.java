package com.projeto.services;

import com.projeto.mappers.CustodiaMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.projeto.services.exceptions.ObjectNotFoundException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaMovimentacaoCustodiaServiceTest {
    final CarteiraRepository carteiras=mock(CarteiraRepository.class);
    final MovimentacaoCustodiaRepository movimentos=mock(MovimentacaoCustodiaRepository.class);
    final EstornoMovimentacaoCustodiaRepository estornos=mock(EstornoMovimentacaoCustodiaRepository.class);
    final ConsultaMovimentacaoCustodiaService service=new ConsultaMovimentacaoCustodiaService(carteiras,movimentos,estornos,new CustodiaMapper());
    @Test void filtroInvalidoPrecedeReferenciaAusenteEIdentificaCampo(){
        var erro=assertThrows(ApiException.class,()->service.historico(1L,"transferencia",null));
        assertEquals(400,erro.getStatus().value());assertEquals("REQUEST_INVALIDO",erro.getCode());
        assertTrue(erro.getDetails().containsKey("natureza"));
        verifyNoInteractions(carteiras,movimentos,estornos);
    }
    @ParameterizedTest @ValueSource(strings={"","ativa","INVALIDA"," ATIVA "})
    void situacaoInvalidaMesmoParaEstornoNaoConsultaBanco(String situacao){
        var erro=assertThrows(ApiException.class,()->service.historico(1L,"ESTORNO",situacao));
        assertEquals("REQUEST_INVALIDO",erro.getCode());assertTrue(erro.getDetails().containsKey("situacao"));
        verifyNoInteractions(carteiras,movimentos,estornos);
    }
    @ParameterizedTest @ValueSource(longs={0,-1})
    void idsInvalidosNaoConsultamBanco(long id){
        assertThrows(ApiException.class,()->service.historico(id,null,null));
        assertThrows(ApiException.class,()->service.detalhe(1L,id));
        verifyNoInteractions(carteiras,movimentos,estornos);
    }
    @Test void referenciaAusenteInterrompeAntesDeCarregarHistorico(){
        assertThrows(ObjectNotFoundException.class,()->service.historico(1L,null,null));
        verify(carteiras).existsById(1L);verifyNoInteractions(movimentos,estornos);
    }
    @Test void detalheAusenteNaoConsultaVinculoDeOutraCarteira(){
        when(carteiras.existsById(1L)).thenReturn(true);
        assertThrows(ObjectNotFoundException.class,()->service.detalhe(1L,2L));
        verify(movimentos).buscarPorCarteiraEId(1L,2L);verifyNoInteractions(estornos);
    }
    @Test void falhaAoCarregarVinculosNaoProduzHistoricoParcial(){
        when(carteiras.existsById(1L)).thenReturn(true);
        when(movimentos.listarPorCarteira(1L)).thenReturn(List.of());
        var falha=new IllegalStateException("Indisponível");
        when(estornos.listarPorCarteira(1L)).thenThrow(falha);
        assertSame(falha,assertThrows(IllegalStateException.class,()->service.historico(1L,null,null)));
    }
}
