package br.com.saborecia.vistoria.config;

import br.com.saborecia.vistoria.dominio.extracao.LlmResponseParser;
import br.com.saborecia.vistoria.dominio.extracao.PromptBuilder;
import br.com.saborecia.vistoria.dominio.extracao.RegexExtractor;
import br.com.saborecia.vistoria.dominio.regras.MotorDeRegras;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Registra no Spring as classes de domínio, que são Java puro (sem anotações) e por isso fáceis de testar.
 */
@Configuration
public class BeansConfig {

    @Bean
    public MotorDeRegras motorDeRegras() {
        return MotorDeRegras.padrao();
    }

    @Bean
    public RegexExtractor regexExtractor() {
        return new RegexExtractor();
    }

    @Bean
    public LlmResponseParser llmResponseParser(ObjectMapper mapper) {
        return new LlmResponseParser(mapper);
    }

    @Bean
    public PromptBuilder promptBuilder(ResourceLoader loader, VistoriaProperties props) throws IOException {
        Resource r = loader.getResource(props.ia().prompt());
        return new PromptBuilder(r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** "Hoje" da aplicação: data fixa de demonstração, se configurada, ou a data do relógio. */
    @Bean
    public Supplier<LocalDate> hoje(VistoriaProperties props) {
        Clock clock = Clock.systemDefaultZone();
        return () -> props.dataReferencia() != null ? props.dataReferencia() : LocalDate.now(clock);
    }

    /** Pool limitado para OCR: o Tesseract usa muita CPU, não deixamos rodar sem limite. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ocrExecutor(VistoriaProperties props) {
        return Executors.newFixedThreadPool(Math.max(1, props.ocr().maxParalelo()));
    }
}
