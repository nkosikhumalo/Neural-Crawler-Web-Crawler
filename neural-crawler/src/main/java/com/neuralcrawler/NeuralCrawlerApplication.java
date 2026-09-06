
package com.neuralcrawler;
//defines the package structure for our java application 

import org.springframework.boot.SpringApplication;
// it imports the core springboot class that boots up the application

import org.springframework.boot.autoconfigure.SpringBootApplication; 
//this annotation enables springboot auto configuration

@SpringBootApplication
public class NeuralCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeuralCrawlerApplication.class, args);
	}

}