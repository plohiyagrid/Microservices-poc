package plohiya.inventory_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import plohiya.inventory_service.model.Inventory;
import plohiya.inventory_service.repository.InventoryRepository;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {

        loadEnvFile();
        SpringApplication.run(InventoryServiceApplication.class, args);
	}

    private static void loadEnvFile() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();

            // Set database properties from .env file
            String dbUrl = dotenv.get("DB_URL");
            String dbUsername = dotenv.get("DB_USERNAME");
            String dbPassword = dotenv.get("DB_PASSWORD");

            if (dbUrl != null) System.setProperty("DB_URL", dbUrl);
            if (dbUsername != null) System.setProperty("DB_USERNAME", dbUsername);
            if (dbPassword != null) System.setProperty("DB_PASSWORD", dbPassword);

            System.out.println("Loaded database configuration from .env file");
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file - " + e.getMessage());
        }
    }

}
