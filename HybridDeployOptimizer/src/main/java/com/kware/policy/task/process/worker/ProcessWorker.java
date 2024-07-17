/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kware.policy.task.process.worker;

import com.kware.policy.common.QueueManager;

import lombok.extern.slf4j.Slf4j;

/**
 * collectMain > collectWorker에서 수집후 입력된 큐에서 데이터를 take하면서 파싱한다.
 */

@Slf4j
@SuppressWarnings("rawtypes")
public class ProcessWorker extends Thread {
	// QueueManager 인스턴스 가져오기
	QueueManager qm = QueueManager.getInstance();
	//멀티스레드에서 사용하기에 안전함

	
	boolean isRunning = false;

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public void run() {
		isRunning = true;

	}
	
	

	
	/**
	 * main test
	 * @param args
	 */
	public static void main(String[] args) {
			
	}
	
	
}
