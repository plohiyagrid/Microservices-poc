package plohiya.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.stream.DoubleStream;

@Configuration
public class webClientConfig {

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }
}
