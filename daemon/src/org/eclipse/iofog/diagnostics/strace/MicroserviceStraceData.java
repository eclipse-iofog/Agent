/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.diagnostics.strace;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MicroserviceStraceData {

	private final String microserviceUuid;
	private int pid;
	private final AtomicBoolean straceRun = new AtomicBoolean();
	private List<String> resultBuffer = new CopyOnWriteArrayList<>();

	public MicroserviceStraceData(String microserviceUuid, int pid, boolean straceRun) {
		this.microserviceUuid = microserviceUuid;
		this.pid = pid;
		this.straceRun.set(straceRun);
	}

	public String getMicroserviceUuid() {
		return microserviceUuid;
	}

	public List<String> getResultBuffer() {
		return resultBuffer;
	}

	public void setResultBuffer(List<String> resultBuffer) {
		this.resultBuffer = resultBuffer;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public AtomicBoolean getStraceRun() {
		return straceRun;
	}

	public void setStraceRun(boolean straceRun) {
		this.straceRun.set(straceRun);
	}

	@Override
	public String toString() {
		return "MicroserviceStraceData{" +
				"microserviceUuid='" + microserviceUuid + '\'' +
				", pid=" + pid +
				", straceRun=" + straceRun +
				", resultBuffer=" + resultBuffer +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MicroserviceStraceData that = (MicroserviceStraceData) o;
		return pid == that.pid &&
				Objects.equals(microserviceUuid, that.microserviceUuid) &&
				Objects.equals(straceRun, that.straceRun) &&
				Objects.equals(resultBuffer, that.resultBuffer);
	}

	@Override
	public int hashCode() {

		return Objects.hash(microserviceUuid, pid, straceRun, resultBuffer);
	}

	public String getResultBufferAsString() {
		StringBuilder stringBuilder = new StringBuilder("");
		for (String line: this.resultBuffer) {
			stringBuilder.append(line).append("\n");
		}
		return stringBuilder.toString();
	}
}
