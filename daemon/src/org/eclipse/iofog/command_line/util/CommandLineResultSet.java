package org.eclipse.iofog.command_line.util;

import java.util.function.Function;

/**
 * Created by ekrylovich
 * on 2/7/18.
 */
public class CommandLineResultSet<E, V> {
	private final E error;
	private final V value;

	public CommandLineResultSet(V value, E error) {
		this.value = value;
		this.error = error;
	}

	public E getError() {
		return error;
	}

	public V getValue() {
		return value;
	}

	public <M,T> CommandLineResultSet<M, T> map(Function<CommandLineResultSet<E, V>, CommandLineResultSet<M, T>> mapper){
		return mapper.apply(this);
	}

	@Override
	public String toString() {
		return "error=" + error +
				", value=" + value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CommandLineResultSet<?, ?> that = (CommandLineResultSet<?, ?>) o;

		if (error != null ? !error.equals(that.error) : that.error != null) return false;
		return value != null ? value.equals(that.value) : that.value == null;
	}

	@Override
	public int hashCode() {
		int result = error != null ? error.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}
}
