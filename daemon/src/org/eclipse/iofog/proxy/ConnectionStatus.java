package org.eclipse.iofog.proxy;

/**
 * Enum that indicates if ssh tunnel is open, closed or failed to start.
 *
 * @author epankov
 *
 */
public enum ConnectionStatus {
    OPEN,
    FAILED,
    CLOSED
}
