package com.sophub.controller;

import com.sophub.model.AIRequest;
import com.sophub.model.AIResponse;
import com.sophub.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sop/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generieren")
    public ResponseEntity<?> generieren(@RequestBody AIRequest anfrage) {
        try {
            String antwort = aiService.generiereAntwort(anfrage.getText());
            return ResponseEntity.ok(new AIResponse(antwort));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(e.getMessage());
        }
    }
}
