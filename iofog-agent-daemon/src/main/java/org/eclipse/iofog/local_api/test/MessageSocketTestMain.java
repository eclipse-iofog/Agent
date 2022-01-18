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
package org.eclipse.iofog.local_api.test;

public class MessageSocketTestMain {

	public static void main(String[] args) throws Exception {
		
	/////////////////////////Open socket and receive message//////////////////////////
		MessageWebsocketReceiverClient rec1 = new MessageWebsocketReceiverClient("LmMgpZGqJk8H4c42RW4RW4");
		Thread t11 = new Thread(rec1);
		t11.start();

		MessageWebsocketReceiverClient rec2 = new MessageWebsocketReceiverClient("pTn6CCZnLKJxZ6wFwcYCR3");
		Thread t12 = new Thread(rec2);
		t12.start();

		MessageWebsocketReceiverClient rec3 = new MessageWebsocketReceiverClient("bbRxKWjnW2t8yrYPYPwJMq");
		Thread t13 = new Thread(rec3);
		t13.start();

		MessageWebsocketReceiverClient rec4 = new MessageWebsocketReceiverClient("wtzNQQ2rrbBjHkdYG9Z8gx");
		Thread t14 = new Thread(rec4);
		t14.start();

		MessageWebsocketReceiverClient rec5 = new MessageWebsocketReceiverClient("yfmw3QdPgHhNv8Fvy6F8nk");
		Thread t15 = new Thread(rec5);
		t15.start();

		MessageWebsocketReceiverClient rec6 = new MessageWebsocketReceiverClient("tBj8KhfZMLd8ggYt6d8mZC");
		Thread t16 = new Thread(rec6);
		t16.start();

		MessageWebsocketReceiverClient rec7 = new MessageWebsocketReceiverClient("vF4ZJJ862zGjHpzVpmFBvV");
		Thread t17 = new Thread(rec7);
		t17.start();

		MessageWebsocketReceiverClient rec8 = new MessageWebsocketReceiverClient("QrmVDvgD4Jg8HYQP4QpJ84");
		Thread t18 = new Thread(rec8);
		t18.start();

		MessageWebsocketReceiverClient rec9 = new MessageWebsocketReceiverClient("7HYv3C4wxVWQNyk3bpKrxr");
		Thread t19 = new Thread(rec9);
		t19.start();

		MessageWebsocketReceiverClient rec10 = new MessageWebsocketReceiverClient("J7cyYQjzRHKYVNGbjpjdBC");
		Thread t20 = new Thread(rec10);
		t20.start();	
		
		//Open socket and send message////////////////////////////////////
		MessageWebsocketSenderClient sender1 = new MessageWebsocketSenderClient("68jrzddRXc92jHm8kPT7CP");
		Thread t1 = new Thread(sender1);
		t1.start();

		MessageWebsocketSenderClient sender2 = new MessageWebsocketSenderClient("XdhkQpqfdpGHV8tthYWxDV");
		Thread t2 = new Thread(sender2);
		t2.start();

		MessageWebsocketSenderClient sender3 = new MessageWebsocketSenderClient("Yy2fxWL9PBFpVDRwyr2FgT");
		Thread t3 = new Thread(sender3);
		t3.start();

		MessageWebsocketSenderClient sender4 = new MessageWebsocketSenderClient("qkxXLyBZqbtyXmkBjFbNZY");
		Thread t4 = new Thread(sender4);
		t4.start();

		MessageWebsocketSenderClient sender5 = new MessageWebsocketSenderClient("Cv9MTMDYmCxmP2hz979Mv3");
		Thread t5 = new Thread(sender5);
		t5.start();

		MessageWebsocketSenderClient sender6 = new MessageWebsocketSenderClient("PgyYmGKPJ99f6TLGTbPkHM");
		Thread t6 = new Thread(sender6);
		t6.start();

		MessageWebsocketSenderClient sender7 = new MessageWebsocketSenderClient("G2mXGwLFvzV2xgGg6pqdCw");
		Thread t7 = new Thread(sender7);
		t7.start();

		MessageWebsocketSenderClient sender8 = new MessageWebsocketSenderClient("7m9YwYjcKXc6VRBFx3dq3K");
		Thread t8 = new Thread(sender8);
		t8.start();

		MessageWebsocketSenderClient sender9 = new MessageWebsocketSenderClient("GJYK6pXGR3222xYjJJgVM9");
		Thread t9 = new Thread(sender9);
		t9.start();

		MessageWebsocketSenderClient sender10 = new MessageWebsocketSenderClient("QxzkZvBVBy8QRXdhcqFQ7z");
		Thread t10 = new Thread(sender10);
		t10.start();		
	
	}

}