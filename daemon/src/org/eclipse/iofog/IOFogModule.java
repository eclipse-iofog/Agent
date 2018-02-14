package org.eclipse.iofog;

import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Common Interface for all ioFog modules
 *
 * @since 1/25/18.
 * @author ekrylovich
 */
public interface IOFogModule {

    void start() throws Exception;
    int getModuleIndex();
    String getModuleName();

    default void logInfo(String message) {
        LoggingService.logInfo(this.getModuleName(), message);
    }

    default void logWarning(String message) {
        LoggingService.logWarning(this.getModuleName(), message);
    }
}
