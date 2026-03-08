package com.reviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({AppConfig.GitHubProperties.class, AppConfig.AzureOpenAIProperties.class})
public class AppConfig {

    @Bean
    public WebClient gitHubWebClient(GitHubProperties gh) {
        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + gh.token())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    public WebClient azureOpenAIWebClient(AzureOpenAIProperties azure) {
        String baseUrl = azure.endpoint() + "openai/deployments/"
                + azure.deploymentName() + "/chat/completions?api-version="
                + azure.apiVersion();
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("api-key", azure.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @ConfigurationProperties(prefix = "github")
    public record GitHubProperties(String token, String webhookSecret) {}

    @ConfigurationProperties(prefix = "azure.openai")
    public record AzureOpenAIProperties(
            String apiKey,
            String endpoint,
            String deploymentName,
            String apiVersion
    ) {}
}