package com.facility.management.infrastructure.jira;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class JiraClient {
    
    private static final Logger logger = LoggerFactory.getLogger(JiraClient.class);
    
    private final WebClient webClient;
    private final String jiraBaseUrl;
    private final String username;
    private final String apiToken;
    
    public JiraClient(@Value("${jira.base-url:https://your-domain.atlassian.net}") String jiraBaseUrl,
                      @Value("${jira.username:}") String username,
                      @Value("${jira.api-token:}") String apiToken) {
        this.jiraBaseUrl = jiraBaseUrl;
        this.username = username;
        this.apiToken = apiToken;
        
        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        this.webClient = WebClient.builder()
                .baseUrl(jiraBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    public Mono<JiraSearchResponse> searchRecentTickets(int maxResults) {
        String jql = "key = SCRUM-21 OR created >= -7d ORDER BY created DESC";
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/3/search")
                        .queryParam("jql", jql)
                        .queryParam("maxResults", maxResults)
                        .queryParam("fields", "summary,description,issuetype,status,priority,assignee,reporter,created,updated,customfield_10000")
                        .build())
                .retrieve()
                .bodyToMono(JiraSearchResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.getIssues() != null) {
                        logger.info("Retrieved {} tickets from JIRA", response.getIssues().size());
                        response.getIssues().forEach(ticket -> 
                            logger.debug("Found ticket: {} - {} (created: {})", 
                                ticket.getKey(), 
                                ticket.getFields() != null ? ticket.getFields().getSummary() : "No summary",
                                ticket.getFields() != null ? ticket.getFields().getCreated() : "No date")
                        );
                    } else {
                        logger.info("Retrieved 0 tickets from JIRA");
                    }
                })
                .doOnError(error -> logger.error("Error retrieving tickets from JIRA", error));
    }
    
    public Mono<Void> addComment(String issueKey, String comment) {
        Map<String, Object> commentBody = Map.of(
                "body", Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", List.of(Map.of(
                                "type", "paragraph",
                                "content", List.of(Map.of(
                                        "type", "text",
                                        "text", comment
                                ))
                        ))
                )
        );
        
        return webClient.post()
                .uri("/rest/api/3/issue/{issueKey}/comment", issueKey)
                .bodyValue(commentBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("Added comment to ticket {}", issueKey))
                .doOnError(error -> logger.error("Error adding comment to ticket {}", issueKey, error));
    }
    
    public Mono<Void> updateTicketStatus(String issueKey, String transitionId) {
        Map<String, Object> transitionBody = Map.of(
                "transition", Map.of("id", transitionId)
        );
        
        return webClient.post()
                .uri("/rest/api/3/issue/{issueKey}/transitions", issueKey)
                .bodyValue(transitionBody)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("Updated status of ticket {}", issueKey))
                .doOnError(error -> logger.error("Error updating status of ticket {}", issueKey, error));
    }
    
    public boolean isConfigured() {
        return username != null && !username.isEmpty() && 
               apiToken != null && !apiToken.isEmpty() &&
               !jiraBaseUrl.contains("your-domain");
    }
}
