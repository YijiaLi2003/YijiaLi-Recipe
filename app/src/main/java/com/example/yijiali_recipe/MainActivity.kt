package com.example.yijiali_recipe

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class RecipeSearchResponse(
    val results: List<Recipe>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

data class Recipe(
    val id: Int,
    val title: String,
    val image: String?
)

data class RecipeDetails(
    val id: Int,
    val title: String,
    val image: String?,
    val instructions: String?,
    val extendedIngredients: List<Ingredient>
)

data class Ingredient(
    val id: Int,
    val original: String
)

interface SpoonacularApi {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String?,
        @Query("includeIngredients") includeIngredients: String?,
        @Query("cuisine") cuisine: String?,
        @Query("diet") diet: String?,
        @Query("maxCalories") maxCalories: Int?,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): RecipeSearchResponse

    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): RecipeDetails
}

class RecipeViewModel : ViewModel() {
    private val apiKey = "02e08f9c51cf4e61a27ab43dbd98f289"

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> get() = _recipes

    private val _selectedRecipe = MutableStateFlow<RecipeDetails?>(null)
    val selectedRecipe: StateFlow<RecipeDetails?> get() = _selectedRecipe

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spoonacular.com/")
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            )
        )
        .build()

    private val api = retrofit.create(SpoonacularApi::class.java)

    fun searchRecipes(
        query: String?,
        ingredients: String?,
        cuisine: String?,
        diet: String?,
        maxCalories: Int?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.searchRecipes(
                    apiKey = apiKey,
                    query = query,
                    includeIngredients = ingredients,
                    cuisine = cuisine,
                    diet = diet,
                    maxCalories = maxCalories
                )
                _recipes.value = response.results
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getRecipeDetails(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val details = api.getRecipeDetails(id, apiKey)
                _selectedRecipe.value = details
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }
}

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val recipeViewModel: RecipeViewModel = viewModel()
                RecipeFinderApp(recipeViewModel)
            }
        }
    }
}

// Composable functions
@Composable
fun RecipeFinderApp(viewModel: RecipeViewModel) {
    val recipes by viewModel.recipes.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape mode
        Row(modifier = Modifier.fillMaxSize()) {
            if (selectedRecipe != null) {
                RecipeList(
                    recipes = recipes,
                    onRecipeClick = { recipe ->
                        viewModel.getRecipeDetails(recipe.id)
                    },
                    modifier = Modifier.weight(1f)
                )
                RecipeDetailsScreen(
                    recipe = selectedRecipe!!,
                    onBack = { viewModel.clearSelectedRecipe() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                SearchInputs(
                    onSearch = { query, ingredients, cuisine, diet, maxCalories ->
                        viewModel.searchRecipes(query, ingredients, cuisine, diet, maxCalories)
                    },
                    modifier = Modifier.weight(1f)
                )
                RecipeList(
                    recipes = recipes,
                    onRecipeClick = { recipe ->
                        viewModel.getRecipeDetails(recipe.id)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        if (selectedRecipe != null) {
            RecipeDetailsScreen(
                recipe = selectedRecipe!!,
                onBack = { viewModel.clearSelectedRecipe() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            RecipeSearchScreen(
                recipes = recipes,
                onSearch = { query, ingredients, cuisine, diet, maxCalories ->
                    viewModel.searchRecipes(query, ingredients, cuisine, diet, maxCalories)
                },
                onRecipeClick = { recipe ->
                    viewModel.getRecipeDetails(recipe.id)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun RecipeSearchScreen(
    recipes: List<Recipe>,
    onSearch: (String?, String?, String?, String?, Int?) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SearchInputs(onSearch = onSearch)
        RecipeList(recipes = recipes, onRecipeClick = onRecipeClick)
    }
}

@Composable
fun SearchInputs(
    onSearch: (String?, String?, String?, String?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var diet by remember { mutableStateOf("") }
    var maxCalories by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Recipe Finder", style = MaterialTheme.typography.headlineMedium)

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cuisine,
                        onValueChange = { cuisine = it },
                        label = { Text("Cuisine") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxCalories,
                        onValueChange = { maxCalories = it },
                        label = { Text("Max Calories") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("Ingredients") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = diet,
                        onValueChange = { diet = it },
                        label = { Text("Diet") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cuisine,
                onValueChange = { cuisine = it },
                label = { Text("Cuisine") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = diet,
                onValueChange = { diet = it },
                label = { Text("Diet") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = maxCalories,
                onValueChange = { maxCalories = it },
                label = { Text("Max Calories") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val maxCal = maxCalories.toIntOrNull()
                onSearch(
                    query.ifBlank { null },
                    ingredients.ifBlank { null },
                    cuisine.ifBlank { null },
                    diet.ifBlank { null },
                    maxCal
                )
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search")
        }
    }
}

@Composable
fun RecipeList(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(recipes) { recipe ->
            RecipeListItem(recipe = recipe, onClick = { onRecipeClick(recipe) })
        }
    }
}
@Composable
fun RecipeListItem(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (recipe.image != null) {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = recipe.title,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = "No image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(recipe.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Composable
fun RecipeDetailsScreen(
    recipe: RecipeDetails,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (recipe.image != null) {
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "No image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ingredients", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        recipe.extendedIngredients.forEach { ingredient ->
            Text("- ${ingredient.original}", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Instructions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            recipe.instructions?.replace(Regex("<.*?>"), "") ?: "No instructions available.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/*
Tested on:
- Pixel 8a emulator (Android 13) in both portrait and landscape modes.
- Pixel Tablet emulator (Android 13) in both portrait and landscape modes.
*/
