package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regressão do caminho civil direto em Ação, histórico, SQL DATE e API. */
@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReferenciaDataJpaDiagnosticoTest {
    @Autowired AcaoRepository acoes;
    @Autowired HistoricoCotacaoRepository historicos;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired AcaoMapper mapper;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @MethodSource("civilDates")
    void rastreiaEscritaLeituraEntidadeEApi(String text) throws Exception {
        LocalDate original = LocalDate.parse(text);
        OffsetDateTime obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
        Acao a = acoes.saveAndFlush(new Acao("D" + UUID.randomUUID().toString().substring(0, 20), "Diagnostico",
                Mercado.EUA, Moeda.USD, BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, original, obtained));
        HistoricoCotacao h = historicos.saveAndFlush(new HistoricoCotacao(a, BigDecimal.TEN, obtained,
                ReferenciaTipoCotacao.DATA, null, original, obtained));
        assertEquals(original, mapper.toResponse(a).referenciaMercadoCotacao().data());
        Long aid = a.getId(), hid = h.getId();
        em.clear();
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            Long id = table.equals("acao") ? aid : hid;
            jdbc.query("select referencia_data, cast(referencia_data as varchar) from " + table + " where id = ?", r -> {
                LocalDate direct = r.getObject(1, LocalDate.class);
                assertEquals(java.sql.Types.DATE, r.getMetaData().getColumnType(1));
                assertEquals(original, direct, "SQL DATE deve preservar calendário proléptico");
                assertEquals(text, r.getString(2));
                String legacy;
                try { legacy = r.getDate(1).toLocalDate().toString(); }
                catch (DateTimeException e) { legacy = "ERROR:" + e.getMessage(); }
                System.out.printf("JPA_PROBE db=%s table=%s input=%s sql=%s jdbcDate=%s jdbcLocalDate=%s reinterpretation=%s%n",
                        r.getStatement().getConnection().getMetaData().getDatabaseProductName(), table, original,
                        r.getString(2), legacy, direct, !direct.toString().equals(legacy));
            }, id);
        }
        LocalDate action = acoes.findById(aid).orElseThrow().getReferenciaData();
        LocalDate history = historicos.findById(hid).orElseThrow().getReferenciaData();
        assertEquals(original, action);
        assertEquals(original, history);
        assertEquals(original, mapper.toResponse(acoes.findById(aid).orElseThrow()).referenciaMercadoCotacao().data());
        mvc.perform(get("/acoes/{id}", aid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(text));
        System.out.printf("ENTITY_API_PROBE input=%s action=%s history=%s api=%s%n", original, action, history, action);
    }

    static Stream<String> civilDates() {
        return Stream.concat(Stream.of("0001-01-01", "0004-02-29", "0100-03-01", "0400-02-29",
                        "1500-03-01", "1600-02-29", "1900-03-01", "2000-02-29", "2026-10-02", "9999-12-31"),
                IntStream.rangeClosed(1, 31).mapToObj(day -> LocalDate.of(1582, 10, day).toString()));
    }
}
