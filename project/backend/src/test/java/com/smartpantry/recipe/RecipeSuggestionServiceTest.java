package com.smartpantry.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;

import com.smartpantry.item.Category;
import com.smartpantry.item.ItemQueryService;
import com.smartpantry.item.ItemView;

class RecipeSuggestionServiceTest {

    private static final String CATALOGUE = """
            [
              {"name": "Omelette", "ingredients": ["Eggs", "Milk", "Cheese"]},
              {"name": "Pancakes", "ingredients": ["Eggs", "Flour", "Milk", "Sugar"]},
              {"name": "Toast", "ingredients": ["Bread", "Butter"]},
              {"name": "Soup", "ingredients": ["Carrot", "Onion"]}
            ]
            """;

    @Mock
    private ItemQueryService itemQueryService;

    private RecipeSuggestionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        RecipeCatalogue catalogue = new RecipeCatalogue(
                new ByteArrayResource(CATALOGUE.getBytes(StandardCharsets.UTF_8)));
        service = new RecipeSuggestionService(catalogue, itemQueryService);
    }

    private void pantry(ItemView... items) {
        when(itemQueryService.findAllWithStatus()).thenReturn(List.of(items));
    }

    private static ItemView item(long id, String name, String status) {
        return new ItemView(id, name, 1, Category.Dairy, LocalDate.of(2026, 11, 1), null, status);
    }

    private RecipeSuggestion recipe(String name) {
        return service.suggest().stream()
                .filter(r -> r.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private List<String> names() {
        return service.suggest().stream().map(RecipeSuggestion::name).toList();
    }

    @Test
    void fullyMakeableRecipeHasNoMissingIngredients() {
        pantry(item(1, "Eggs", "valid"), item(2, "Milk", "valid"), item(3, "Cheese", "valid"));

        RecipeSuggestion omelette = recipe("Omelette");

        assertTrue(omelette.makeable());
        assertEquals(List.of(), omelette.missingIngredients());
        assertEquals(List.of("Eggs", "Milk", "Cheese"), omelette.matchedIngredients());
        assertEquals(List.of("Eggs", "Milk", "Cheese"), omelette.ingredients());
    }

    @Test
    void partiallyMakeableRecipeListsTheRemainingIngredientsAsMissing() {
        pantry(item(1, "Eggs", "valid"));

        RecipeSuggestion omelette = recipe("Omelette");

        assertFalse(omelette.makeable());
        assertEquals(List.of("Eggs"), omelette.matchedIngredients());
        assertEquals(List.of("Milk", "Cheese"), omelette.missingIngredients());
    }

    @Test
    void recipeWithNoPantryMatchIsNotMakeableAndAllIngredientsAreMissing() {
        pantry(item(1, "Salt", "valid"));

        RecipeSuggestion toast = recipe("Toast");

        assertFalse(toast.makeable());
        assertEquals(List.of(), toast.matchedIngredients());
        assertEquals(List.of("Bread", "Butter"), toast.missingIngredients());
    }

    @Test
    void matchingIgnoresLetterCase() {
        pantry(item(1, "EGGS", "valid"), item(2, "milk", "valid"), item(3, "cHeEsE", "valid"));

        RecipeSuggestion omelette = recipe("Omelette");

        assertTrue(omelette.makeable());
        assertEquals(List.of(), omelette.missingIngredients());
    }

    @Test
    void matchingDoesNotTrimSpaces() {
        pantry(item(1, " Eggs", "valid"), item(2, "Milk ", "valid"), item(3, "Cheese", "valid"));

        RecipeSuggestion omelette = recipe("Omelette");

        assertFalse(omelette.makeable());
        assertEquals(List.of("Cheese"), omelette.matchedIngredients());
        assertEquals(List.of("Eggs", "Milk"), omelette.missingIngredients());
    }

    @Test
    void recipeUsesExpiringSoonWhenAMatchedItemIsExpiring() {
        pantry(item(1, "Eggs", "expiring"));

        assertTrue(recipe("Omelette").usesExpiringSoon());
    }

    @Test
    void recipeDoesNotUseExpiringSoonWhenMatchedItemsAreValidOrNone() {
        pantry(item(1, "Eggs", "valid"), item(2, "Bread", "none"));

        assertFalse(recipe("Omelette").usesExpiringSoon());
        assertFalse(recipe("Toast").usesExpiringSoon());
    }

    @Test
    void expiringRecipeIsListedBeforeRecipeWithFewerMissingIngredients() {
        // Omelette uses the expiring Eggs and misses 1 (Cheese). Toast is fully makeable (0 missing)
        // but uses nothing expiring, so it must rank after Omelette.
        pantry(item(1, "Eggs", "expiring"), item(2, "Milk", "valid"), item(3, "Bread", "valid"),
                item(4, "Butter", "valid"));

        List<RecipeSuggestion> ranked = service.suggest();

        assertEquals("Omelette", ranked.get(0).name());
        assertEquals(List.of("Cheese"), ranked.get(0).missingIngredients());
        assertTrue(ranked.get(0).usesExpiringSoon());
        RecipeSuggestion toast = recipe("Toast");
        assertTrue(toast.makeable());
        assertFalse(toast.usesExpiringSoon());
        assertTrue(names().indexOf("Toast") > names().indexOf("Omelette"));
    }

    @Test
    void withEqualExpiryUseFewerMissingIngredientsComesFirst() {
        pantry(item(1, "Eggs", "expiring"), item(2, "Milk", "valid"));

        // Omelette misses 1 (Cheese); Pancakes misses 2 (Flour, Sugar). Both use the expiring Eggs.
        assertEquals(List.of("Omelette", "Pancakes"), names().subList(0, 2));
    }

    @Test
    void equalKeysSortNamesAscendingCaseInsensitive() {
        when(itemQueryService.findAllWithStatus()).thenReturn(List.of());
        RecipeCatalogue catalogue = new RecipeCatalogue(new ByteArrayResource("""
                [
                  {"name": "soup", "ingredients": ["Stock", "Leek"]},
                  {"name": "Banana Cake", "ingredients": ["Banana", "Flour"]},
                  {"name": "apricot tart", "ingredients": ["Apricot", "Sugar"]}
                ]
                """.getBytes(StandardCharsets.UTF_8)));
        RecipeSuggestionService sorting = new RecipeSuggestionService(catalogue, itemQueryService);

        List<String> order = sorting.suggest().stream().map(RecipeSuggestion::name).toList();

        // Case-sensitive order would put "Banana Cake" first.
        assertEquals(List.of("apricot tart", "Banana Cake", "soup"), order);
    }

    @Test
    void nameMatchingBothAnExpiringAndAValidItemCountsAsExpiringUse() {
        pantry(item(1, "Eggs", "valid"), item(2, "eggs", "expiring"));

        assertTrue(recipe("Omelette").usesExpiringSoon());
    }

    @Test
    void emptyPantryGivesNoMakeableRecipeAndFullMissingLists() {
        pantry();

        List<RecipeSuggestion> all = service.suggest();

        assertEquals(4, all.size());
        assertTrue(all.stream().noneMatch(RecipeSuggestion::makeable));
        assertEquals(List.of("Eggs", "Milk", "Cheese"), recipe("Omelette").missingIngredients());
        assertEquals(List.of("Carrot", "Onion"), recipe("Soup").missingIngredients());
    }

    @Test
    void whenNoRecipeMatchesEveryRecipeIsStillShownWithAllIngredientsMissing() {
        pantry(item(1, "Salt", "expiring"));

        List<RecipeSuggestion> all = service.suggest();

        assertEquals(4, all.size());
        assertTrue(all.stream().noneMatch(RecipeSuggestion::makeable));
        assertTrue(all.stream().noneMatch(RecipeSuggestion::usesExpiringSoon));
        assertEquals(List.of("Bread", "Butter"), recipe("Toast").missingIngredients());
    }
}
