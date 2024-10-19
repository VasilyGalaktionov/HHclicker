package com.example.demo.controller;

import com.example.demo.model.JobSearchRequest;
import com.example.demo.service.JobSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")  // Разрешить запросы с localhost:3000
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<String> searchJobs(@RequestBody JobSearchRequest request) {
        try {
            // Передаем данные из объекта request в сервис
            jobSearchService.searchAndApply(
                    request.getEmail(),
                    request.getPassword(),
                    request.getQuery(),
                    request.getApiKey(),
                    request.getGptPrompt()  // Добавляем GPT Prompt
            );
            return ResponseEntity.ok("Search and application process started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " + e.getMessage());
        }
    }
}