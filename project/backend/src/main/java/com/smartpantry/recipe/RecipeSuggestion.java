package com.smartpantry.recipe;

import java.util.List;

/**
 * One recipe as the pantry sees it. matchedIngredients and missingIngredients
 * partition the recipe's ingredients. makeable is true only when nothing is
 * missing. usesExpiringSoon is true when a matched pantry item has status
 * "expiring".
 */
public record RecipeSuggestion(
        String name,
        List<String> ingredients,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        boolean makeable,
        boolean usesExpiringSoon) {
}
