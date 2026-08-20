package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "corretora",
        uniqueConstraints = @UniqueConstraint(name = "uk_corretora_cnpj", columnNames = "cnpj")
)
public class Corretora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 255)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(length = 30)
    private String numero;

    @Column(length = 255)
    private String complemento;

    @Column(nullable = false, length = 150)
    private String bairro;

    @Column(nullable = false, length = 150)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "situacao_cadastral", nullable = false, length = 100)
    private String situacaoCadastral;

    @Column(name = "validada_mercado_financeiro", nullable = false)
    private boolean validadaMercadoFinanceiro;

    @Column(name = "data_cadastro", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataCadastro;

    protected Corretora() {
    }

    public Corretora(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String email,
            String telefone,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String situacaoCadastral,
            OffsetDateTime dataCadastro
    ) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.email = email;
        this.telefone = telefone;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.situacaoCadastral = situacaoCadastral;
        this.validadaMercadoFinanceiro = false;
        this.dataCadastro = dataCadastro;
    }

    public Long getId() {
        return id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getSituacaoCadastral() {
        return situacaoCadastral;
    }

    public boolean isValidadaMercadoFinanceiro() {
        return validadaMercadoFinanceiro;
    }

    public OffsetDateTime getDataCadastro() {
        return dataCadastro;
    }
}
