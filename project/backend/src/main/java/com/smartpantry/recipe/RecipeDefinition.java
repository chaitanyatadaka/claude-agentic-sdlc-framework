package com.smartpantry.recipe;

import java.util.List;

/**
 * A predefined recipe: a display name and the plain ingredient names it needs.
 * Ingredients carry no quantities (A-003).
 */
public record RecipeDefinition(String name, List<String> ingredients) {
}
