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
package org.eclipse.iofog.message_bus;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * class to generate unique id for {@link Message}
 * 
 * @author saeid
 *
 */
public class MessageIdGenerator {
	private static final char[] ALPHABETS_ARRAY = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz123456789".toCharArray();

	/**
	 * converts base 10 to base 58
	 *
	 * @param number - number to be converted
	 * @return base 58 presentation of number
	 */
	private String toBase58(long number) {
		StringBuilder result = new StringBuilder();
		while (number >= 58) {
			result.append(ALPHABETS_ARRAY[(int) (number % 58)]);
			number /= 58;
		}
		result.append(ALPHABETS_ARRAY[(int) number]);
		return result.reverse().toString();
	}


	private final int MAX_SEQUENCE = 100000000;
	private volatile long lastTime = 0;
	private volatile int sequence = MAX_SEQUENCE;
	/**
	 * generates unique id based on time and sequence
	 *
	 * @param time - timestamp in milliseconds
	 */
	public synchronized String generate(long time) {
		if (lastTime == time) {
			sequence--;
		} else {
			lastTime = time;
			sequence = MAX_SEQUENCE;
		}
		return toBase58(time) + toBase58(sequence);
	}


	// uuid
	private static final int PRE_GENERATED_IDS_COUNT = 100_000;
	private boolean isRefilling = false;
	private final Queue<String> generatedIds = new LinkedList<>();
	/**
	 * generates unique id based on UUID
	 * 
	 */
	private final Runnable refill = () -> {
		if (isRefilling)
			return;
		isRefilling = true;
		while (generatedIds.size() < PRE_GENERATED_IDS_COUNT)
			synchronized (generatedIds) {
				generatedIds.offer(UUID.randomUUID().toString().replaceAll("-", ""));
			}
		isRefilling = false;
	};
	
	/**
	 * returns next generated id from list
	 * 
	 * @return id
	 */
	public String getNextId() {
		while (generatedIds.size() == 0);
		synchronized (generatedIds) {
			return generatedIds.poll();
		}
	}
	
	public MessageIdGenerator() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(refill, 0, 5, TimeUnit.SECONDS);
	}
	
//			 			 1         2         3         4         5         6         7         8         9         0         1         2         3         4         5         6         7         
//  			12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//  Double:		uWJ7hf5NAL7ufAjUbfwAfAwuwQfLNN9y9wyUQy5syYE3W9qJsWC3bEQu7WYUWJyUYNu3EbSfbCLWwdNsGsEYuqY35h79YoSqYh7bfYWGGNmWqYyWoummsdwodoqLyjGSwyfWhu3hb1Q1J9wWhdUbJufo9AACYJyuYG3E5mmTre6jpcs
//	Float:		319jQQdmU33UhsMFqAEuBx
//  Long:		XZvFwUyHzQM
//	Integer:	DRvaEH
//	Elements:	rv8H3m2fdzKJrKNVGftmWFYDbvgb3tpv
}
