package plohiya.inventory_service.exception;

public class InvalidInventoryRequestException extends RuntimeException {
    public InvalidInventoryRequestException(String message) {
        super(message);
    }
}