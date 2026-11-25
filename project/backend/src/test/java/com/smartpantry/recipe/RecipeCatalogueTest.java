package com.smartpantry.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;

class RecipeCatalogueTest {

    @TempDir
    Path dir;

    @Test
    void bundledFileLoadsTenRecipesWithNameAndIngredients() {
        List<RecipeDefinition> recipes = new RecipeCatalogue().all();

        assertThat(recipes).hasSize(10);
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.name()).isNotBlank();
            assertThat(recipe.ingredients()).isNotEmpty();
        }
    }

    @Test
    void returnedListsCannotBeModified() {
        List<RecipeDefinition> recipes = new RecipeCatalogue().all();

        assertThatThrownBy(() -> recipes.add(new RecipeDefinition("Extra", List.of("Egg"))))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> recipes.get(0).ingredients().add("Extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void missingFileStopsStartup() {
        FileSystemResource missing = new FileSystemResource(dir.resolve("absent.json"));

        assertThatThrownBy(() -> new RecipeCatalogue(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recipe file not found");
    }

    @Test
    void malformedJsonStopsStartup() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[{\"name\": \"Toast\", ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed");
    }

    @Test
    void emptyArrayStopsStartup() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains no recipes");
    }

    @Test
    void recipeWithoutNameStopsStartupAndNamesItsPosition() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[{\"ingredients\": [\"Egg\"]}]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at position 1 has no name");
    }

    @Test
    void recipeWithoutIngredientsStopsStartupAndNamesTheRecipe() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[{\"name\": \"Toast\"}]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'Toast' has no ingredients");
    }

    @Test
    void recipeWithEmptyIngredientListStopsStartupAndNamesTheRecipe() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[{\"name\": \"Toast\", \"ingredients\": []}]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'Toast' has no ingredients");
    }

    @Test
    void blankIngredientStopsStartupAndNamesTheRecipe() {
        assertThatThrownBy(() -> new RecipeCatalogue(write("[{\"name\": \"Toast\", \"ingredients\": [\"Bread\", \" \"]}]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'Toast' has a blank ingredient");
    }

    private FileSystemResource write(String json) {
        try {
            Path file = Files.createTempFile(dir, "recipes", ".json");
            Files.writeString(file, json, StandardCharsets.UTF_8);
            return new FileSystemResource(file);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
