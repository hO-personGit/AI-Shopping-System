package org.example.springboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiRestTemplateConfig {

    @Bean
    public RestTemplate aiRestTemplate(@Value("${ai.service.timeout:45000}") int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 全链路 TraceId 透传：调用 AI 微服务时带上 X-Trace-Id
        restTemplate.getInterceptors().add((ClientHttpRequestInterceptor) (httpRequest, body, execution) -> {
            String traceId = org.slf4j.MDC.get(org.example.springboot.filter.TraceIdFilter.TRACE_ID_MDC_KEY);
            if (traceId != null && !traceId.isBlank()) {
                httpRequest.getHeaders().set(org.example.springboot.filter.TraceIdFilter.TRACE_ID_HEADER, traceId);
            }
            return execution.execute(httpRequest, body);
        });
        return restTemplate;
    }
}