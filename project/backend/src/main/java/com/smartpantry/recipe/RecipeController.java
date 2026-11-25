package com.smartpantry.recipe;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recipe suggestions, read only (REQ-024). There is no create, update or delete
 * endpoint, so those methods get 405 from Spring MVC.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeSuggestionService suggestionService;

    public RecipeController(RecipeSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<RecipeSuggestion> list() {
        return suggestionService.suggest();
    }
}
