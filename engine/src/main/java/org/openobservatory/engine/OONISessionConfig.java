package org.openobservatory.engine;

import oonimkall.SessionConfig;

/** OONISessionConfig contains configuration for a OONISession. */
public final class OONISessionConfig {
    /**
     * assetsDir is a mandatory setting specifying the directory in
     * which you want the OONI Probe engine to store the assets.
     */
    String assetsDir;

    /**
     * logger is an optional setting specifying a logger where
     * to send log messages generated by OONI Probe's engine.
     */
    OONILogger logger;

    /**
     * proxy contains a string describing the optional proxy
     * that we should be using. If the string is empty, no
     * proxy will actually be used.
     *
     * Examples:
     *
     * 1. `psiphon:///`
     *
     * 2. `socks5://10.0.0.1:9050`
     *
     * The first example will use the embedded psiphon
     * configuration, if possible. The latter will
     * instead use a specific socks5 proxy running on
     * another machine (e.g. a Tor process)
     */
    String proxy;

    /**
     * ProbeServicesURL allows you to optionally force the
     * usage of an alternative probe services. This setting
     * should only be used to implement integration tests.
     */
    String probeServicesURL;

    /**
     * softwareName is a mandatory setting specifying the name of
     * the application that is using OONI Probe's engine.
     */
    String softwareName;

    /**
     * softwareVersion is a mandatory setting specifying the version of
     * the application that is using OONI Probe's engine.
     */
    String softwareVersion;

    /**
     * stateDir is a mandatory setting specifying the directory in
     * which you want the OONI Probe engine to store its state.
     */
    String stateDir;

    /**
     * tempDir is a mandatory setting specifying the directory in which
     * you want the OONI Probe engine to store temporary files.
     */
    String tempDir;

    /**
     * tunnelDir is the mandatory setting specifying the directory
     * in which you want OONI to store persistent tunnel state.
     */
    String tunnelDir;

    /**
     * verbose controls whether to emit debug messages. This setting only
     * has effect if you also specify a Logger using setLogger.
     */
    boolean verbose;

    protected SessionConfig toOonimkallSessionConfig() {
        SessionConfig c = new SessionConfig();
        c.setAssetsDir(assetsDir);
        c.setProxy(proxy);
        c.setLogger(new PELoggerAdapter(logger));
        c.setProbeServicesURL(probeServicesURL);
        c.setSoftwareName(softwareName);
        c.setSoftwareVersion(softwareVersion);
        c.setStateDir(stateDir);
        c.setTempDir(tempDir);
        c.setTunnelDir(tunnelDir);
        c.setVerbose(verbose);
        return c;
    }
}
