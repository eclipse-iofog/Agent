/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.logging.LoggingService.logDebug;

/**
 * provides methods for "number <=> byte array" conversions
 * 
 * @author saeid
 *
 */
public class BytesUtil {

	private static final String MODULE_NAME = "BytesUtil";

	public static byte[] copyOfRange(byte[] src, int from, int to) {
		return (from < 0 || from >= src.length || to < from || to > src.length) ? new byte[]{} : Arrays.copyOfRange(src, from, to);
	}
	
	public static byte[] longToBytes(long x) {
		logDebug(MODULE_NAME, "Inside longToBytes");
		byte[] b = new byte[8];
		for (int i = 0; i < 8; ++i) {
			b[i] = (byte) (x >> (8 - i - 1 << 3));
		}
		logDebug(MODULE_NAME, "Finished longToBytes");
		return b;
	}

	public static long bytesToLong(byte[] bytes) {
		logDebug(MODULE_NAME, "Inside bytesToLong");
		long result = 0;
		for (byte aByte : bytes) {
			result = (result << 8) + (aByte & 0xff);
		}
		logDebug(MODULE_NAME, "Finished bytesToLong");
		return result;
	}

	public static byte[] integerToBytes(int x) {
		logDebug(MODULE_NAME, "Inside integerToBytes");
		byte[] b = new byte[4];
		for (int i = 0; i < 4; ++i) {
			b[i] = (byte) (x >> (4 - i - 1 << 3));
		}
		logDebug(MODULE_NAME, "Finished integerToBytes");
		return b;
	}

	public static int bytesToInteger(byte[] bytes) {
		logDebug(MODULE_NAME, "Inside bytesToInteger");
		int result = 0;
		for (byte aByte : bytes) {
			result = (result << 8) + (aByte & 0xff);
		}
		logDebug(MODULE_NAME, "Finished bytesToInteger");
		return result;
	}

	public static byte[] shortToBytes(short x) {
		logDebug(MODULE_NAME, "Inside shortToBytes");
		byte[] b = new byte[2];
		for (int i = 0; i < 2; ++i) {
			b[i] = (byte) (x >> (2 - i - 1 << 3));
		}
		logDebug(MODULE_NAME, "Finished shortToBytes");
		return b;
	}

	public static short bytesToShort(byte[] bytes) {
		logDebug(MODULE_NAME, "Finished bytesToShort");
		short result = 0;
		for (byte aByte : bytes) {
			result = (short) ((result << 8) + (aByte & 0xff));
		}
		logDebug(MODULE_NAME, "Finished bytesToShort");
		return result;
	}

	public static byte[] stringToBytes(String s) {
		logDebug(MODULE_NAME, "Inside stringToBytes");
		if (s == null) {
			logDebug(MODULE_NAME, "Finished stringToBytes");
			return new byte[] {};
		}		
		else {
			logDebug(MODULE_NAME, "Finished stringToBytes");
			return s.getBytes(UTF_8);
		}
			
	}

	public static String bytesToString(byte[] bytes) {
		logDebug(MODULE_NAME, "Create bytesToString");
		return new String(bytes);
	}

	/**
	 * returns string presentation of byte array
	 * byte[] a = {1, 2, 3, 4} => String a = "[1, 2, 3, 4]"
	 * 
	 * @param bytes
	 * @return string
	 */
	public static String byteArrayToString(byte[] bytes) {
		logDebug(MODULE_NAME, "Create byteArrayToString");
		StringBuilder result = new StringBuilder();

		result.append("[");
		for (byte b : bytes) {
			if (result.length() > 1)
				result.append(", ");
			result.append(b);
		}
		result.append("]");
		logDebug(MODULE_NAME, "Finished byteArrayToString");
		return result.toString();
	}
}
