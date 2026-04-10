package com.yourorg.aiknowledgeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.yourorg")
public class AiKnowledgeAssistantApplication {

	public static void main(String[] args) {
		long maxHeap = Runtime.getRuntime().maxMemory()/1024/1024;
		System.out.println("MAX HEAP : " + maxHeap + "MB");
		SpringApplication.run(AiKnowledgeAssistantApplication.class, args);
		System.out.println("Backend Running");
	}

}
