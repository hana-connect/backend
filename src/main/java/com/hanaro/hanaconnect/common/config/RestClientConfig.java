package com.hanaro.hanaconnect.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

	@Bean
	public RestClient.Builder restClientBuilder() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(30));

		return RestClient.builder()
			.requestFactory(requestFactory);
	}
}
