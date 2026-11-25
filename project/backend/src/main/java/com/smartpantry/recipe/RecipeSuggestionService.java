package com.smartpantry.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.smartpantry.item.ItemQueryService;
import com.smartpantry.item.ItemView;

/**
 * Suggests recipes from the pantry (REQ-020 to REQ-023). Pantry items come from
 * ItemQueryService.findAllWithStatus(), which already holds each item's status.
 * This service has no expiry logic of its own: it reads the status string only.
 *
 * Matching is by ingredient name, case-insensitive, exact, with no trimming
 * (A-003). Every catalogue recipe is returned (A-012), ranked by: usesExpiringSoon
 * first, then fewest missing ingredients, then name ascending case-insensitive.
 */
@Service
public class RecipeSuggestionService {

    private final RecipeCatalogue catalogue;
    private final ItemQueryService itemQueryService;

    public RecipeSuggestionService(RecipeCatalogue catalogue, ItemQueryService itemQueryService) {
        this.catalogue = catalogue;
        this.itemQueryService = itemQueryService;
    }

    public List<RecipeSuggestion> suggest() {
        Set<String> pantryNames = new HashSet<>();
        Set<String> expiringNames = new HashSet<>();
        for (ItemView item : itemQueryService.findAllWithStatus()) {
            String key = key(item.name());
            pantryNames.add(key);
            if (ItemView.STATUS_EXPIRING.equals(item.status())) {
                expiringNames.add(key);
            }
        }

        List<RecipeSuggestion> suggestions = new ArrayList<>();
        for (RecipeDefinition recipe : catalogue.all()) {
            suggestions.add(suggestFor(recipe, pantryNames, expiringNames));
        }
        suggestions.sort(RANKING);
        return List.copyOf(suggestions);
    }

    private static RecipeSuggestion suggestFor(RecipeDefinition recipe,
                                               Set<String> pantryNames, Set<String> expiringNames) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        boolean usesExpiring = false;
        for (String ingredient : recipe.ingredients()) {
            String key = key(ingredient);
            if (pantryNames.contains(key)) {
                matched.add(ingredient);
                usesExpiring |= expiringNames.contains(key);
            } else {
                missing.add(ingredient);
            }
        }
        return new RecipeSuggestion(
                recipe.name(),
                recipe.ingredients(),
                List.copyOf(matched),
                List.copyOf(missing),
                missing.isEmpty(),
                usesExpiring);
    }

    /** Case-insensitive comparison key. No trimming, so surrounding spaces count (A-003). */
    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static final Comparator<RecipeSuggestion> RANKING =
            Comparator.comparing(RecipeSuggestion::usesExpiringSoon, Comparator.reverseOrder())
                    .thenComparingInt(recipe -> recipe.missingIngredients().size())
                    .thenComparing(recipe -> key(recipe.name()));
}
