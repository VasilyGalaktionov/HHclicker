package com.example.demo.model;

public class JobSearchRequest {
    private String email;
    private String password;
    private String query;
    private String location;
    private String apiKey;  // Поле для API-ключа
    private String gptPrompt;  // Поле для GPT запроса

    // Геттеры и сеттеры

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGptPrompt() {
        return gptPrompt;
    }

    public void setGptPrompt(String gptPrompt) {
        this.gptPrompt = gptPrompt;
    }
}