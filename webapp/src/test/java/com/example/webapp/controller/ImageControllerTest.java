package com.example.webapp.controller;

import com.example.webapp.entities.User;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ImageControllerTest {

    @Autowired
    UserController userController;
    @Autowired
    RecipeController recipeController;
    @Autowired
    ImageController imageController;
    @Autowired
    S3Hanlder s3Hanlder;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockMultipartFile mockMultipartFile;

    String recipeString;
    Gson gson;
    String recipeId;
    String imageId;

    @Before
    @Transactional
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");
        mockMultipartFile = new MockMultipartFile("file", "time.jpg", "", readBytesFromFile("time.jpg"));

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
        recipeId = gson.fromJson(result, JsonObject.class).get("id").getAsString();
        request.setRequestURI("/v1/recipe/" + recipeId + "/image");
        result = imageController.postImage(mockMultipartFile, request, response);
        imageId = gson.fromJson(result, JsonObject.class).get("id").getAsString();
    }

    @After
    public void clean() {

        s3Hanlder.deletefile(imageId);
    }


    @Test
    @Transactional
    public void postImage() {

        request.setRequestURI("/v1/recipe/" + recipeId + "/image");
        String result = imageController.postImage(mockMultipartFile, request, response);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(result, JsonObject.class);
        s3Hanlder.deletefile(jsonObject.get("id").getAsString());

    }

    @Test
    @Transactional
    public void deleteImage() {
        request.setRequestURI("/v1/recipe/" + recipeId + "/image/" + imageId);
        imageController.deleteImage(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        s3Hanlder.uploadfile(mockMultipartFile, imageId);
    }

    @Test
    @Transactional
    public void getImage() {
        request.setRequestURI("/v1/recipe/" + recipeId + "/image/" + imageId);
        imageController.getImage(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    //convert the file to byte[]
    private static byte[] readBytesFromFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;

    }
}