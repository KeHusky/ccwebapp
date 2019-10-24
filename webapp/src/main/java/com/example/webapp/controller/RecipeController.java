package com.example.webapp.controller;

import com.example.webapp.dao.ImageRepository;
import com.example.webapp.dao.RecipeRepository;
import com.example.webapp.entities.DummyRecipe;
import com.example.webapp.entities.Image;
import com.example.webapp.entities.Recipe;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.Helper;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
public class RecipeController {

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private S3Hanlder s3Hanlder;

    @Autowired
    Helper helper;

    Gson gson = new Gson();
    JsonObject jsonObject = new JsonObject();

    @RequestMapping(value = "/v1/recipe", method = RequestMethod.POST, produces = "application/json")
    protected String postRecipe(@RequestBody String recipe_string, HttpServletRequest request, HttpServletResponse response) {

        DummyRecipe dummyRecipe = gson.fromJson(recipe_string, DummyRecipe.class);

        if (dummyRecipe.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipe.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipe.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipe.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipe.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipe.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipe.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipe.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipe.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return jsonObject.toString();
        }

        String header = request.getHeader("Authorization");
        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {

                String now = new Date().toString();
                Recipe recipe = new Recipe();
                recipe.setCreated_ts(now);
                recipe.setUpdatedTs(now);
                recipe.setAuthor_id(user.getId());
                recipe.setCook_time_in_min(Integer.parseInt(dummyRecipe.getCook_time_in_min()));
                recipe.setPrep_time_in_min(Integer.parseInt(dummyRecipe.getPrep_time_in_min()));
                recipe.setTotal_time_in_min(recipe.getCook_time_in_min() + recipe.getPrep_time_in_min());
                recipe.setTitle(dummyRecipe.getTitle());
                recipe.setCusine(dummyRecipe.getCusine());
                recipe.setServings(Integer.parseInt(dummyRecipe.getServings()));
                recipe.setIngredients(gson.toJson(dummyRecipe.getIngredients()));
                recipe.setSteps(gson.toJson(dummyRecipe.getSteps()));
                recipe.setNutrition_information(gson.toJson(dummyRecipe.getNutrition_information()));

                recipeRepository.save(recipe);
                jsonObject.addProperty("id", recipe.getId());
                jsonObject.addProperty("created_ts", recipe.getCreated_ts());
                jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
                jsonObject.addProperty("author_id", recipe.getAuthor_id());
                jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
                jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
                jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
                jsonObject.addProperty("title", recipe.getTitle());
                jsonObject.addProperty("cusine", recipe.getCusine());
                jsonObject.addProperty("servings", recipe.getServings());
                jsonObject.addProperty("ingrediets", recipe.getIngredients());
                jsonObject.addProperty("steps", recipe.getSteps());
                jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

                jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
                jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
                jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));
                response.setStatus(HttpServletResponse.SC_CREATED);
            }
        }
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.DELETE, produces = "application/json")
    protected String deleteRecipe(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();
        String recipeID = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                Optional<Recipe> r = recipeRepository.findById(recipeID);
                Recipe recipe = r.isPresent() ? r.get() : null;
                if (recipe == null) {
                    jsonObject.addProperty("error message", "no such recipe");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipe.getAuthor_id().equals(user.getId())) {
                    jsonObject.addProperty("error message", "no access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    List<Image> imageList = imageRepository.findByRecipeId(recipeID);
                    for (Image image : imageList) {
                        s3Hanlder.deletefile(image.getId());
                        imageRepository.delete(image);
                    }
                    recipeRepository.delete(recipe);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            }

        }

        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.PUT, produces = "application/json")
    protected String putRecipe(@RequestBody String recipe_string, HttpServletRequest request, HttpServletResponse response) {

        DummyRecipe dummyRecipe = gson.fromJson(recipe_string, DummyRecipe.class);

        if (dummyRecipe.getCook_time_in_min() == null)
            jsonObject.addProperty("error message", "cook_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getCook_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "cook_time_in_min is not multiple of 5");
        if (dummyRecipe.getPrep_time_in_min() == null)
            jsonObject.addProperty("error message", "prep_time_in_min not contained");
        else if (Integer.parseInt(dummyRecipe.getPrep_time_in_min()) % 5 != 0)
            jsonObject.addProperty("error message", "prep_time_in_min is not multiple of 5");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "title not contained");
        if (dummyRecipe.getTitle() == null)
            jsonObject.addProperty("error message", "cusine not contained");
        if (dummyRecipe.getServings() == null)
            jsonObject.addProperty("error message", "servings not contained");
        else if (Integer.parseInt(dummyRecipe.getServings()) < 1)
            jsonObject.addProperty("error message", "servings minimum is 1");
        else if (Integer.parseInt(dummyRecipe.getServings()) > 5)
            jsonObject.addProperty("error message", "servings maximum is 5");
        if (dummyRecipe.getIngredients() == null)
            jsonObject.addProperty("error message", "ingredients not contained");
        if (dummyRecipe.getSteps() == null)
            jsonObject.addProperty("error message", "steps not contained");
        else if (dummyRecipe.getSteps().length < 1)
            jsonObject.addProperty("error message", "steps minimum is 1");
        if (dummyRecipe.getNutrition_information() == null)
            jsonObject.addProperty("error message", "nutrition_information not contained");

        if (jsonObject.get("error message") != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return jsonObject.toString();
        }

        JsonObject jsonObject = new JsonObject();
        String recipeID = request.getRequestURI().split("/")[3];
        String header = request.getHeader("Authorization");

        if (header == null) {
            jsonObject.addProperty("error message", "no auth");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            User user = helper.validateUser(header);
            if (user == null) {
                jsonObject.addProperty("error message", "wrong username or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                Optional<Recipe> r = recipeRepository.findById(recipeID);
                Recipe recipe = r.isPresent() ? r.get() : null;
                if (recipe == null) {
                    jsonObject.addProperty("error message", "no such recipe");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipe.getAuthor_id().equals(user.getId())) {
                    jsonObject.addProperty("error message", "no access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {

                    recipe.setUpdatedTs(new Date().toString());
                    recipe.setCook_time_in_min(Integer.parseInt(dummyRecipe.getCook_time_in_min()));
                    recipe.setPrep_time_in_min(Integer.parseInt(dummyRecipe.getPrep_time_in_min()));
                    recipe.setTotal_time_in_min(recipe.getCook_time_in_min() + recipe.getPrep_time_in_min());
                    recipe.setTitle(dummyRecipe.getTitle());
                    recipe.setCusine(dummyRecipe.getCusine());
                    recipe.setServings(Integer.parseInt(dummyRecipe.getServings()));
                    recipe.setIngredients(gson.toJson(dummyRecipe.getIngredients()));
                    recipe.setSteps(gson.toJson(dummyRecipe.getSteps()));
                    recipe.setNutrition_information(gson.toJson(dummyRecipe.getNutrition_information()));

                    recipeRepository.save(recipe);
                    jsonObject.addProperty("id", recipe.getId());
                    jsonObject.addProperty("created_ts", recipe.getCreated_ts());
                    jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
                    jsonObject.addProperty("author_id", recipe.getAuthor_id());
                    jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
                    jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
                    jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
                    jsonObject.addProperty("title", recipe.getTitle());
                    jsonObject.addProperty("cusine", recipe.getCusine());
                    jsonObject.addProperty("servings", recipe.getServings());
                    jsonObject.addProperty("ingrediets", recipe.getIngredients());
                    jsonObject.addProperty("steps", recipe.getSteps());
                    jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

                    jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
                    jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
                    jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }

        }

        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.GET, produces = "application/json")
    public String getRecipe(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();

        String recipeID = request.getRequestURI().split("/")[3];

        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeID);
        Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            List<Image> imageList = imageRepository.findByRecipeId(recipeID);
            JsonArray jsonArray = new JsonArray();
            for (Image image : imageList) {
                JsonObject j = new JsonObject();
                j.addProperty("id", image.getId());
                j.addProperty("url", image.getUrl());
                jsonArray.add(j);
            }
            jsonObject.add("image", jsonArray);
            jsonObject.addProperty("id", recipe.getId());
            jsonObject.addProperty("created_ts", recipe.getCreated_ts());
            jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
            jsonObject.addProperty("author_id", recipe.getAuthor_id());
            jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
            jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
            jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
            jsonObject.addProperty("title", recipe.getTitle());
            jsonObject.addProperty("cusine", recipe.getCusine());
            jsonObject.addProperty("servings", recipe.getServings());
            jsonObject.addProperty("ingrediets", recipe.getIngredients());
            jsonObject.addProperty("steps", recipe.getSteps());
            jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

            jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
            jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
            jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

            response.setStatus(HttpServletResponse.SC_OK);
        }

        return jsonObject.toString();

    }

    @RequestMapping(value = "/v1/recipes", method = RequestMethod.GET, produces = "application/json")
    public String getNewestRecipe(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();

        List<Recipe> recipeList = recipeRepository.findAll(new Sort(Sort.Direction.DESC, "updatedTs"));
        Recipe recipe = recipeList.get(0);

        List<Image> imageList = imageRepository.findByRecipeId(recipe.getId());
        JsonArray jsonArray = new JsonArray();
        for (Image image : imageList) {
            JsonObject j = new JsonObject();
            j.addProperty("id", image.getId());
            j.addProperty("url", image.getUrl());
            jsonArray.add(j);
        }
        jsonObject.add("image", jsonArray);
        jsonObject.addProperty("id", recipe.getId());
        jsonObject.addProperty("created_ts", recipe.getCreated_ts());
        jsonObject.addProperty("updated_ts", recipe.getUpdatedTs());
        jsonObject.addProperty("author_id", recipe.getAuthor_id());
        jsonObject.addProperty("cook_time_in_min", recipe.getCook_time_in_min());
        jsonObject.addProperty("prep_time_in_time", recipe.getPrep_time_in_min());
        jsonObject.addProperty("total_time_in_min", recipe.getTotal_time_in_min());
        jsonObject.addProperty("title", recipe.getTitle());
        jsonObject.addProperty("cusine", recipe.getCusine());
        jsonObject.addProperty("servings", recipe.getServings());
        jsonObject.addProperty("ingrediets", recipe.getIngredients());
        jsonObject.addProperty("steps", recipe.getSteps());
        jsonObject.addProperty("nutrition_information", recipe.getNutrition_information());

        jsonObject.add("ingrediets", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("steps", gson.fromJson(recipe.getIngredients(), JsonArray.class));
        jsonObject.add("nutrition_information", gson.fromJson(recipe.getNutrition_information(), JsonObject.class));

        response.setStatus(HttpServletResponse.SC_OK);

        return jsonObject.toString();

    }

}
