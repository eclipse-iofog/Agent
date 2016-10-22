package com.iotracks.iofog.element;

/**
 * represents IOElements port mappings
 * 
 * @author saeid
 *
 */
public class PortMapping {
	final String outside;
	final String inside;

	public PortMapping(String outside, String inside) {
		this.outside = outside;
		this.inside = inside;
	}

	public String getOutside() {
		return outside;
	}

	public String getInside() {
		return inside;
	}

	@Override
	public String toString() {
		return "{" + "outside='" + outside + '\'' + ", inside='" + inside + '\'' + '}';
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PortMapping))
			return false;
		
		PortMapping o = (PortMapping) other;
		return this.outside.equals(o.outside) && this.inside.equals(o.inside);
	}

}
