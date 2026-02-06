package plohiya.product_service.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/product-service}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "product-service";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        System.out.println("Creating MongoClient with URI: " + mongoUri.substring(0, Math.min(50, mongoUri.length())) + "...");
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .build();
        
        return MongoClients.create(settings);
    }
}
