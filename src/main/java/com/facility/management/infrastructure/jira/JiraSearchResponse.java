package com.facility.management.infrastructure.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchResponse {
    
    private List<JiraTicket> issues;
    private int total;
    private int startAt;
    private int maxResults;
    
    public List<JiraTicket> getIssues() { return issues; }
    public void setIssues(List<JiraTicket> issues) { this.issues = issues; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    
    public int getStartAt() { return startAt; }
    public void setStartAt(int startAt) { this.startAt = startAt; }
    
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
