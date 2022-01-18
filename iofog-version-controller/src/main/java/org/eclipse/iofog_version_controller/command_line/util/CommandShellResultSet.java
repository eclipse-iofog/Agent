/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog_version_controller.command_line.util;

import java.util.function.Function;

/**
 * Created by ekrylovich
 * on 2/7/18.
 */
public class CommandShellResultSet<E, V> {
	private final E error;
	private final V value;

	public CommandShellResultSet(V value, E error) {
		this.value = value;
		this.error = error;
	}

	public E getError() {
		return error;
	}

	public V getValue() {
		return value;
	}

	public <M,T> CommandShellResultSet<M, T> map(Function<CommandShellResultSet<E, V>, CommandShellResultSet<M, T>> mapper){
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

		CommandShellResultSet<?, ?> that = (CommandShellResultSet<?, ?>) o;

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
