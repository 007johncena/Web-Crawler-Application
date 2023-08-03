package com.Crawler.CrawlerApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com"})
public class CrawlerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrawlerAppApplication.class, args);
	}

}
