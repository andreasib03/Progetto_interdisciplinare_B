package it.uninsubria.shared.rmi;

public class ServiceException extends java.rmi.RemoteException {
    public ServiceException(String message) {
        super(message);
    }
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
