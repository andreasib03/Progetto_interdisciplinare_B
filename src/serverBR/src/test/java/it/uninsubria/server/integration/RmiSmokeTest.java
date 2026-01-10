package it.uninsubria.server.integration;

import static org.junit.Assert.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;

import org.junit.Test;

public class RmiSmokeTest {

    interface TestService extends Remote {
        String echo(String msg) throws RemoteException;
    }

    static class TestServiceImpl extends UnicastRemoteObject implements TestService {
        protected TestServiceImpl() throws RemoteException { super(); }
        @Override
        public String echo(String msg) throws RemoteException { return msg; }
    }

    @Test
    public void testInProcessRmiSmoke() throws Exception {
        Registry registry = LocateRegistry.createRegistry(1099);
        TestService svc = new TestServiceImpl();
        registry.rebind("TestService", svc);

        Registry registryLookup = LocateRegistry.getRegistry("localhost", 1099);
        TestService remote = (TestService) registryLookup.lookup("TestService");
        String resp = remote.echo("ping");
        assertEquals("ping", resp);

        // cleanup
        registry.unbind("TestService");
        UnicastRemoteObject.unexportObject(svc, true);
    }
}
