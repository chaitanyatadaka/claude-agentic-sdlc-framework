package com.smartpantry.recipe;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The read-only catalogue of predefined recipes, loaded from recipes.json at
 * startup (DR-009). A missing, malformed or invalid file stops startup with a
 * logged reason. There is no add or edit method (Q-011, REQ-024).
 */
@Component
public class RecipeCatalogue {

    static final String RESOURCE_NAME = "recipes.json";

    private static final Logger LOG = LoggerFactory.getLogger(RecipeCatalogue.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<RecipeDefinition> recipes;

    public RecipeCatalogue() {
        this(new ClassPathResource(RESOURCE_NAME));
    }

    RecipeCatalogue(Resource resource) {
        this.recipes = load(resource);
    }

    /** The predefined recipes in file order. The returned list cannot be modified. */
    public List<RecipeDefinition> all() {
        return recipes;
    }

    private static List<RecipeDefinition> load(Resource resource) {
        if (!resource.exists()) {
            throw startupFailure("recipe file not found: " + resource.getDescription());
        }
        RecipeDefinition[] parsed;
        try (InputStream in = resource.getInputStream()) {
            parsed = MAPPER.readValue(in, RecipeDefinition[].class);
        } catch (IOException e) {
            throw startupFailure("recipe file is malformed or unreadable: " + resource.getDescription());
        }
        if (parsed == null || parsed.length == 0) {
            throw startupFailure("recipe file contains no recipes");
        }

        List<RecipeDefinition> validated = new ArrayList<>(parsed.length);
        for (int i = 0; i < parsed.length; i++) {
            RecipeDefinition recipe = parsed[i];
            String position = "at position " + (i + 1);
            if (recipe == null) {
                throw startupFailure("recipe " + position + " is empty");
            }
            if (recipe.name() == null || recipe.name().isBlank()) {
                throw startupFailure("recipe " + position + " has no name");
            }
            String label = "'" + recipe.name() + "'";
            if (recipe.ingredients() == null || recipe.ingredients().isEmpty()) {
                throw startupFailure("recipe " + label + " has no ingredients");
            }
            for (String ingredient : recipe.ingredients()) {
                if (ingredient == null || ingredient.isBlank()) {
                    throw startupFailure("recipe " + label + " has a blank ingredient");
                }
            }
            validated.add(new RecipeDefinition(recipe.name(), List.copyOf(recipe.ingredients())));
        }
        return List.copyOf(validated);
    }

    private static IllegalStateException startupFailure(String reason) {
        LOG.error("Recipe catalogue failed to load, startup stopped: {}", reason);
        return new IllegalStateException("Recipe catalogue failed to load: " + reason);
    }
}
