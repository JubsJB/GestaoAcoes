package com.projeto.security;

import com.projeto.services.exceptions.ApiException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/** Após CSRF, antes do binding MVC: conta inclusive JSON ilegível sem guardar o corpo. */
final class RedefinicaoRateLimitFilter extends OncePerRequestFilter {
    private final RedefinicaoRateLimiter limites;
    private final SecurityErrors errors;
    private final JsonMapper mapper;
    RedefinicaoRateLimitFilter(RedefinicaoRateLimiter limites,SecurityErrors errors,JsonMapper mapper) {
        this.limites=limites;this.errors=errors;this.mapper=mapper;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getServletPath().isEmpty()?request.getRequestURI().substring(request.getContextPath().length()):request.getServletPath();
        return !"POST".equals(request.getMethod()) || !path.equals("/auth/redefinicoes/confirmar");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        byte[] corpo;
        try {
            limites.registrarIp(request.getRemoteAddr());
            corpo=request.getInputStream().readNBytes(16385);
            if(corpo.length>16384) {
                errors.write(request,response,HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Corpo da requisição inválido.");return;
            }
            String username=null;
            try {
                var json=mapper.readTree(corpo);
                if(json!=null && json.isObject() && json.get("username")!=null && !json.get("username").isNull()) username=json.get("username").asText();
            } catch(RuntimeException ignorada) {
                // Não encaminhar a falha do parser: o resolver MVC pode logar o token bruto em DEBUG.
                errors.write(request,response,HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Corpo da requisição inválido.");return;
            }
            if(username!=null) limites.registrarIdentidade(username,request.getRemoteAddr());
        } catch(ApiException ex) {
            if(ex.getStatus()==HttpStatus.TOO_MANY_REQUESTS) response.setHeader("Retry-After",ex.getDetails().get("retryAfter").toString());
            errors.write(request,response,ex);return;
        }
        chain.doFilter(new CorpoRepetivel(request,corpo),response);
    }
    private static final class CorpoRepetivel extends HttpServletRequestWrapper {
        private final byte[] corpo;
        CorpoRepetivel(HttpServletRequest request,byte[] corpo) {super(request);this.corpo=corpo;}
        @Override public int getContentLength(){return corpo.length;}
        @Override public long getContentLengthLong(){return corpo.length;}
        @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        @Override public ServletInputStream getInputStream() {
            var stream=new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override public int read(){return stream.read();}
                @Override public boolean isFinished(){return stream.available()==0;}
                @Override public boolean isReady(){return true;}
                @Override public void setReadListener(ReadListener listener){throw new IllegalStateException("Leitura assíncrona indisponível");}
            };
        }
    }
}
