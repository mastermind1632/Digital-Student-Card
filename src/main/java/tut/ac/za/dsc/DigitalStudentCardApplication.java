package tut.ac.za.dsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalStudentCardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalStudentCardApplication.class, args);
    }
}
