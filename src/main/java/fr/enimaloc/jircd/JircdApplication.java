package fr.enimaloc.jircd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JircdApplication {

    public static void main(String[] args) {
        SpringApplication.run(JircdApplication.class, args);
    }

}
