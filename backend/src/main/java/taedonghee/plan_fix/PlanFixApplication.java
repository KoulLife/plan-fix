package taedonghee.plan_fix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PlanFixApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanFixApplication.class, args);
	}

}
