package org.eclipse.iofog.utils.exceptions;

public class UnknownVersionCommandException extends IllegalArgumentException {

    public UnknownVersionCommandException() {
        super("Unknown version changing command");
    }
}
