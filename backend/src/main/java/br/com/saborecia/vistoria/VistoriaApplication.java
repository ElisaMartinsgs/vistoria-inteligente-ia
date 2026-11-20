package br.com.saborecia.vistoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VistoriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VistoriaApplication.class, args);
    }
}
