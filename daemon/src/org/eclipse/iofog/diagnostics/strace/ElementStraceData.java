package org.eclipse.iofog.diagnostics.strace;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ElementStraceData {

	private final String elementId;
	private int pid;
	private final AtomicBoolean strace = new AtomicBoolean();
	private List<String> resultBuffer = new CopyOnWriteArrayList<>();

	public ElementStraceData(String elementId, int pid, boolean strace) {
		this.elementId = elementId;
		this.pid = pid;
		this.strace.set(strace);
	}

	public String getElementId() {
		return elementId;
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

	public AtomicBoolean getStrace() {
		return strace;
	}

	@Override
	public String toString() {
		return "ElementStraceData{" +
				"elementId='" + elementId + '\'' +
				", pid=" + pid +
				", strace=" + strace +
				", resultBuffer=" + resultBuffer +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ElementStraceData that = (ElementStraceData) o;
		return pid == that.pid &&
				Objects.equals(elementId, that.elementId) &&
				Objects.equals(strace, that.strace) &&
				Objects.equals(resultBuffer, that.resultBuffer);
	}

	@Override
	public int hashCode() {

		return Objects.hash(elementId, pid, strace, resultBuffer);
	}

	public String getResultBufferAsString() {
		StringBuilder stringBuilder = new StringBuilder("");
		for (String line: this.resultBuffer) {
			stringBuilder.append(line).append("\n");
		}
		return stringBuilder.toString();
	}
}
