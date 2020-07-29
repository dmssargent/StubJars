package davidsar.gent.stubjars;

import java.security.Permission;

public class StubJarsSecurityManager extends SecurityManager {
    @Override
    public void checkExit(int status) {
        throw new SecurityException();
    }

    @Override
    public void checkPermission(Permission permission) {

    }
}
