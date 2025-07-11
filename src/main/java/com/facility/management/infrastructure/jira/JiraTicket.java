package com.facility.management.infrastructure.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraTicket {
    
    private String id;
    private String key;
    private Fields fields;
    
    public static class Fields {
        private String summary;
        @JsonDeserialize(using = DescriptionDeserializer.class)
        private String description;
        private IssueType issuetype;
        private Status status;
        private Priority priority;
        private User assignee;
        private User reporter;
        private String created;
        private String updated;
        
        @JsonProperty("customfield_10000")
        private String facilityId;
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public IssueType getIssuetype() { return issuetype; }
        public void setIssuetype(IssueType issuetype) { this.issuetype = issuetype; }
        
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        
        public User getAssignee() { return assignee; }
        public void setAssignee(User assignee) { this.assignee = assignee; }
        
        public User getReporter() { return reporter; }
        public void setReporter(User reporter) { this.reporter = reporter; }
        
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        
        public String getUpdated() { return updated; }
        public void setUpdated(String updated) { this.updated = updated; }
        
        public String getFacilityId() { return facilityId; }
        public void setFacilityId(String facilityId) { this.facilityId = facilityId; }
    }
    
    public static class IssueType {
        private String name;
        private String id;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
    
    public static class Status {
        private String name;
        private String id;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
    
    public static class Priority {
        private String name;
        private String id;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
    
    public static class User {
        private String displayName;
        private String emailAddress;
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public Fields getFields() { return fields; }
    public void setFields(Fields fields) { this.fields = fields; }
    
    public boolean isFacilityManagementTicket() {
        if (fields == null) return false;
        
        String summary = fields.getSummary();
        String description = fields.getDescription();
        String issueType = fields.getIssuetype() != null ? fields.getIssuetype().getName() : "";
        
        return containsFacilityKeywords(summary) || 
               containsFacilityKeywords(description) || 
               containsFacilityKeywords(issueType);
    }
    
    private boolean containsFacilityKeywords(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        return lowerText.contains("facility") ||
               lowerText.contains("customer") ||
               lowerText.contains("contract") ||
               lowerText.contains("gfrn") ||
               lowerText.contains("gfcid") ||
               lowerText.contains("cag") ||
               lowerText.contains("accounting period") ||
               lowerText.contains("country of risk");
    }
    
    public static class DescriptionDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override
        public String deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            if (node.isTextual()) {
                return node.asText();
            }
            
            if (node.isObject() && node.has("content")) {
                return extractTextFromContent(node.get("content"));
            }
            
            return node.toString();
        }
        
        private String extractTextFromContent(JsonNode contentArray) {
            StringBuilder text = new StringBuilder();
            
            if (contentArray.isArray()) {
                for (JsonNode item : contentArray) {
                    if (item.has("content")) {
                        text.append(extractTextFromContent(item.get("content")));
                    } else if (item.has("text")) {
                        text.append(item.get("text").asText()).append(" ");
                    }
                }
            }
            
            return text.toString().trim();
        }
    }
}
