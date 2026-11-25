import { useCallback, useEffect, useRef, useState } from 'react';
import { apiRequest, ApiError } from '../api/client.js';
import ErrorMessage from '../components/ErrorMessage.jsx';

// GET /api/recipes: the server ranks the list (recipes using an expiring item first, then fewest
// missing ingredients, then name). The page shows that order as received and does not re-sort.
// Matching and expiry come from the API; the browser does no date or matching logic here.

function matchedOf(recipe) {
  if (Array.isArray(recipe.matchedIngredients)) return recipe.matchedIngredients;
  const missing = Array.isArray(recipe.missingIngredients) ? recipe.missingIngredients : [];
  return recipe.ingredients.filter((ingredient) => !missing.includes(ingredient));
}

function RecipeCard({ recipe }) {
  const missing = Array.isArray(recipe.missingIngredients) ? recipe.missingIngredients : [];
  const matched = new Set(matchedOf(recipe));

  return (
    <li className="card recipe-card">
      <div className="recipe-placeholder" aria-hidden="true">{recipe.name.charAt(0)}</div>
      <div className="recipe-body">
        <div className="recipe-head">
          <h2>{recipe.name}</h2>
          <span className={`badge ${recipe.makeable ? 'badge-valid' : 'badge-none'}`}>
            {recipe.makeable ? 'Makeable' : 'Not makeable'}
          </span>
        </div>

        {recipe.usesExpiringSoon && (
          <p className="badge badge-expiring recipe-expiring">Uses an item expiring soon</p>
        )}

        <h3 className="recipe-subhead">Ingredients</h3>
        <ul className="ingredient-list" aria-label={`Ingredients for ${recipe.name}`}>
          {recipe.ingredients.map((ingredient) => {
            const inPantry = matched.has(ingredient);
            return (
              <li key={ingredient} className={inPantry ? 'is-matched' : 'is-missing'}>
                {inPantry && <span aria-hidden="true">✓ </span>}
                {ingredient}
                {inPantry && <span className="small"> in pantry</span>}
              </li>
            );
          })}
        </ul>

        {missing.length > 0 && (
          <>
            <h3 className="recipe-subhead">Missing</h3>
            <ul className="ingredient-list" aria-label={`Missing ingredients for ${recipe.name}`}>
              {missing.map((ingredient) => (
                <li key={ingredient} className="is-missing">{ingredient}</li>
              ))}
            </ul>
          </>
        )}
      </div>
    </li>
  );
}

export default function RecipesPage() {
  const [state, setState] = useState({ status: 'loading', recipes: [], error: null, correlationId: null });
  const latestRequest = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++latestRequest.current;
    setState({ status: 'loading', recipes: [], error: null, correlationId: null });
    try {
      const data = await apiRequest('/api/recipes');
      if (requestId !== latestRequest.current) return;
      if (!Array.isArray(data)) {
        throw new ApiError({ kind: 'http', message: 'Unexpected response from the server. Try again.' });
      }
      setState({ status: 'ready', recipes: data, error: null, correlationId: null });
    } catch (error) {
      if (requestId !== latestRequest.current) return;
      setState({
        status: 'error',
        recipes: [],
        error: error.message ?? 'Could not load recipes. Try again.',
        correlationId: error.correlationId ?? null,
      });
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function renderBody() {
    if (state.status === 'loading') {
      return <p className="muted empty" role="status">Loading recipes…</p>;
    }
    if (state.status === 'error') {
      return <ErrorMessage message={state.error} correlationId={state.correlationId} onRetry={load} />;
    }
    if (state.recipes.length === 0) {
      return <p className="muted empty">No recipes match your pantry right now.</p>;
    }
    return (
      <ul className="recipe-grid" aria-label="Recipe suggestions">
        {state.recipes.map((recipe) => <RecipeCard key={recipe.name} recipe={recipe} />)}
      </ul>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>Recipes</h1>
        <p className="muted">Recipes you can make from your pantry. Recipes that use expiring items come first.</p>
      </header>
      {renderBody()}
    </section>
  );
}
