package plohiya.product_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ProductServiceApplication {

	public static void main(String[] args) {
		// Load .env file before Spring Boot starts
		loadEnvFile();
		SpringApplication.run(ProductServiceApplication.class, args);
	}

	private static void loadEnvFile() {
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory(System.getProperty("user.dir"))
					.ignoreIfMissing()
					.load();
			
			// Set MONGODB_URI from .env file
			String mongodbUri = dotenv.get("MONGODB_URI");
			if (mongodbUri != null && !mongodbUri.isEmpty()) {
				System.setProperty("MONGODB_URI", mongodbUri);
				System.out.println("Loaded MONGODB_URI from .env file");
			}
		} catch (Exception e) {
			System.err.println("Warning: Could not load .env file - " + e.getMessage());
		}
	}

}
