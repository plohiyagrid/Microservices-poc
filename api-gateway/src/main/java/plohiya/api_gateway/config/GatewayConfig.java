package plohiya.api_gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class GatewayConfig {

    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> httpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(java.time.Duration.ofSeconds(10))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
    }
}