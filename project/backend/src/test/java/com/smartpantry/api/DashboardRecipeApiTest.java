package com.smartpantry.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpantry.item.Category;
import com.smartpantry.item.GroceryItem;
import com.smartpantry.item.ItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardRecipeApiTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String dbFile = tempDir.toAbsolutePath().toString().replace('\\', '/') + "/smartpantry";
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + dbFile);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ItemRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private void seed(String name, int quantity, Category category, LocalDate expiry) {
        repository.save(new GroceryItem(name, quantity, category, LocalDate.now().minusDays(5), expiry));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static void assertNoInternalDetails(String body) {
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("SELECT"), body);
        assertFalse(body.contains("at com."), body);
    }

    @Test
    void dashboardOfEmptyInventoryGivesZeroTotalsAndSixEmptyCategories() throws Exception {
        MvcResult result = mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andExpect(jsonPath("$.expiringSoon").isEmpty())
                .andExpect(jsonPath("$.expired").isEmpty())
                .andExpect(jsonPath("$.categories.length()").value(6))
                .andReturn();

        JsonNode categories = json(result).get("categories");
        assertEquals("Vegetables", categories.get(0).get("category").asText());
        assertEquals("Others", categories.get(5).get("category").asText());
        for (JsonNode category : categories) {
            assertEquals(0, category.get("itemCount").asInt());
            assertEquals(0, category.get("quantityTotal").asLong());
        }
    }

    @Test
    void dashboardTotalsMatchTheSeededInventory() throws Exception {
        seed("Apple", 3, Category.Fruits, LocalDate.now().plusDays(10));
        seed("Milk", 2, Category.Dairy, LocalDate.now());
        seed("Bread", 1, Category.Grains, LocalDate.now().minusDays(1));
        seed("Rice", 5, Category.Grains, null);

        MvcResult result = mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.totalQuantity").value(11))
                .andExpect(jsonPath("$.expiringSoon[0].name").value("Milk"))
                .andExpect(jsonPath("$.expiringSoon[0].status").value("expiring"))
                .andExpect(jsonPath("$.expired[0].name").value("Bread"))
                .andExpect(jsonPath("$.expired[0].status").value("expired"))
                .andReturn();

        JsonNode categories = json(result).get("categories");
        JsonNode grains = null;
        long count = 0;
        long quantity = 0;
        for (JsonNode category : categories) {
            count += category.get("itemCount").asLong();
            quantity += category.get("quantityTotal").asLong();
            if ("Grains".equals(category.get("category").asText())) {
                grains = category;
            }
        }
        assertEquals(4, count);
        assertEquals(11, quantity);
        assertEquals(2, grains.get("itemCount").asInt());
        assertEquals(6, grains.get("quantityTotal").asLong());
    }

    @Test
    void recipesWithEmptyPantryShowEveryRecipeAsNotMakeable() throws Exception {
        MvcResult result = mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].makeable").value(false))
                .andExpect(jsonPath("$[0].usesExpiringSoon").value(false))
                .andExpect(jsonPath("$[0].matchedIngredients").isEmpty())
                .andReturn();

        JsonNode first = json(result).get(0);
        assertEquals(first.get("ingredients"), first.get("missingIngredients"));
    }

    @Test
    void recipesHaveTheRequiredShapeAndAreRankedByExpiryThenMissingThenName() throws Exception {
        seed("Eggs", 6, Category.Dairy, LocalDate.now());
        seed("Milk", 1, Category.Dairy, LocalDate.now().plusDays(10));
        seed("Butter", 1, Category.Dairy, LocalDate.now().plusDays(10));
        seed("Cheese", 1, Category.Dairy, LocalDate.now().plusDays(10));

        MvcResult result = mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cheese Omelette"))
                .andExpect(jsonPath("$[0].makeable").value(true))
                .andExpect(jsonPath("$[0].usesExpiringSoon").value(true))
                .andExpect(jsonPath("$[0].missingIngredients").isEmpty())
                .andExpect(jsonPath("$[0].matchedIngredients[0]").value("Eggs"))
                .andReturn();

        JsonNode list = json(result);
        List<String> names = new ArrayList<>();
        list.forEach(recipe -> names.add(recipe.get("name").asText()));
        assertEquals(List.of("Cheese Omelette", "Scrambled Eggs", "Banana Pancakes", "Vegetable Fried Rice"),
                names.subList(0, 4));

        JsonNode scrambled = list.get(1);
        assertEquals(List.of("Eggs", "Butter", "Milk", "Salt"), textOf(scrambled.get("ingredients")));
        assertEquals(List.of("Salt"), textOf(scrambled.get("missingIngredients")));
        assertFalse(scrambled.get("makeable").asBoolean());
        assertTrue(scrambled.get("usesExpiringSoon").asBoolean());

        for (JsonNode recipe : list) {
            assertTrue(recipe.has("name"));
            assertTrue(recipe.has("ingredients"));
            assertTrue(recipe.has("matchedIngredients"));
            assertTrue(recipe.has("missingIngredients"));
            assertTrue(recipe.has("makeable"));
            assertTrue(recipe.has("usesExpiringSoon"));
            assertEquals(6, recipe.size());
        }
    }

    @Test
    void pantryMatchingNoRecipeStillReturnsEveryRecipe() throws Exception {
        seed("Zebra Fish", 1, Category.Others, LocalDate.now().plusDays(10));

        mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[9].makeable").value(false));
    }

    @Test
    void recipeWriteMethodsAreNotAllowed() throws Exception {
        for (MvcResult result : List.of(
                mvc.perform(post("/api/recipes").contentType("application/json").content("{}")).andReturn(),
                mvc.perform(put("/api/recipes/1").contentType("application/json").content("{}")).andReturn(),
                mvc.perform(delete("/api/recipes/1")).andReturn())) {
            int code = result.getResponse().getStatus();
            assertTrue(code == 404 || code == 405, "unexpected status " + code);
            assertNoInternalDetails(result.getResponse().getContentAsString());
        }
    }

    @Test
    void dashboardAndRecipeResponsesCarryNoInternalDetails() throws Exception {
        String dashboard = mvc.perform(get("/api/dashboard")).andReturn().getResponse().getContentAsString();
        String recipes = mvc.perform(get("/api/recipes")).andReturn().getResponse().getContentAsString();

        assertNoInternalDetails(dashboard);
        assertNoInternalDetails(recipes);
    }

    private static List<String> textOf(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }
}
