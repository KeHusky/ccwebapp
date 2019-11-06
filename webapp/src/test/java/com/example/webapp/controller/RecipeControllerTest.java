package com.example.webapp.controller;

import com.example.webapp.dao.RecipeRepository;
import com.example.webapp.entities.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RecipeControllerTest {

    @Autowired
    RecipeController recipeController;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    UserController userController;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    String recipeString;
    Gson gson;
    String id;

    @Before
    @Transactional
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");

        recipeString = "{\n" +
                "  \"cook_time_in_min\": 15,\n" +
                "  \"prep_time_in_min\": 15,\n" +
                "  \"title\": \"Creamy Cajun Chicken Pasta\",\n" +
                "  \"cusine\": \"Italian\",\n" +
                "  \"servings\": 2,\n" +
                "  \"ingredients\": [\n" +
                "    \"4 ounces linguine pasta\",\n" +
                "    \"2 boneless, skinless chicken breast halves, sliced into thin strips\",\n" +
                "    \"2 teaspoons Cajun seasoning\",\n" +
                "    \"2 tablespoons butter\"\n" +
                "  ],\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"position\": 1,\n" +
                "      \"items\": \"some text here\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nutrition_information\": {\n" +
                "    \"calories\": 100,\n" +
                "    \"cholesterol_in_mg\": 4,\n" +
                "    \"sodium_in_mg\": 100,\n" +
                "    \"carbohydrates_in_grams\": 53.7,\n" +
                "    \"protein_in_grams\": 53.7\n" +
                "  }\n" +
                "}";
        gson = new Gson();

        //prepare database
        String test = "{'username':'yuan@husky.edu','password' : '123abcABC','firstname' : 'Ke','lastname' : 'Yuan'}";
        User user = gson.fromJson(test, User.class);
        userController.createNewUser(user, request, response);
        String result = recipeController.postRecipe(recipeString, request, response);
        id = gson.fromJson(result, JsonObject.class).get("id").getAsString();
    }

    @Test
    @Transactional
    public void postRecipe() {
        String result = recipeController.postRecipe(recipeString, request, response);
        assertEquals(true, result != null);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    }

    @Test
    @Transactional
    public void deleteRecipe() {
        request.setRequestURI("/v1/recipe/"+id);
        recipeController.deleteRecipe(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    }

    @Test
    @Transactional
    public void putRecipe() {
        request.setRequestURI("/v1/recipe/"+id);
        recipeString = "{\n" +
                "  \"cook_time_in_min\": 15,\n" +
                "  \"prep_time_in_min\": 15,\n" +
                "  \"title\": \"Creamy Cajun Chicken Pasta\",\n" +
                "  \"cusine\": \"Italian\",\n" +
                "  \"servings\": 2,\n" +
                "  \"ingredients\": [\n" +
                "    \"4 ounces linguine pasta\",\n" +
                "    \"2 boneless, skinless chicken breast halves, sliced into thin strips\",\n" +
                "    \"2 teaspoons Cajun seasoning\",\n" +
                "    \"2 tablespoons butter\"\n" +
                "  ],\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"position\": 1,\n" +
                "      \"items\": \"updated\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nutrition_information\": {\n" +
                "    \"calories\": 100,\n" +
                "    \"cholesterol_in_mg\": 4,\n" +
                "    \"sodium_in_mg\": 100,\n" +
                "    \"carbohydrates_in_grams\": 53.7,\n" +
                "    \"protein_in_grams\": 53.7\n" +
                "  }\n" +
                "}";
        recipeController.putRecipe(recipeString, request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @Transactional
    public void getRecipe() {
        request.setRequestURI("/v1/recipe/"+id);
        recipeController.getRecipe(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

    }


}