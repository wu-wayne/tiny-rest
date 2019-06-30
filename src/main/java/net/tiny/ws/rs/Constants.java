package net.tiny.ws.rs;

public interface Constants {
    public enum Mode {
        instance,
        singleton
    }
    String CONTEXT_PARAM_APPLICATION = "javax.ws.rs.Application";

    String CONTEXT_SCAN_PACKAGES = "javax.ws.rs.scan.packages";

    String CONTEXT_SCAN_PACKAGES_EXCLUDE  = "javax.ws.rs.scan.packages.exclude";

    String INIT_PARAM_APPLICATION  = CONTEXT_PARAM_APPLICATION;
}
