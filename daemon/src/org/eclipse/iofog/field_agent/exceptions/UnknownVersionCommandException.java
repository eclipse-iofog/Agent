package org.eclipse.iofog.field_agent.exceptions;

public class UnknownVersionCommandException extends IllegalArgumentException {

    public UnknownVersionCommandException() {
        super("Unknown version changing command");
    }
}
