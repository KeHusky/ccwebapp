package com.example.webapp.controller;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.example.webapp.dao.ImageRepository;
import com.example.webapp.dao.MetadataRepository;
import com.example.webapp.dao.RecipeRepository;
import com.example.webapp.entities.Image;
import com.example.webapp.entities.Metadata;
import com.example.webapp.entities.Recipe;
import com.example.webapp.entities.User;
import com.example.webapp.helpers.Helper;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ImageController {

    @Autowired
    Helper helper;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    S3Hanlder s3Hanlder;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    MetadataRepository metadataRepository;

    @RequestMapping(value = "/v1/recipe/{id}/image", method = RequestMethod.POST, produces = "application/json")
    protected String postImage(@RequestParam MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        JsonObject jsonObject = new JsonObject();

        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        if (!(suffix.toLowerCase().equals("png") || suffix.toLowerCase().equals("jpg") || suffix.toLowerCase().equals("jpeg"))) {
            jsonObject.addProperty("error message", "wrong file type");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return jsonObject.toString();
        }

        String recipeId = request.getRequestURI().split("/")[3];
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
                Optional<Recipe> o = recipeRepository.findById(recipeId);
                Recipe recipe = o.isPresent() ? o.get() : null;
                if (recipe == null) {
                    jsonObject.addProperty("error message", "no such recipe");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipe.getAuthor_id().equals(user.getId())) {
                    jsonObject.addProperty("error message", "no access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    Image image = new Image();
                    image.setRecipeId(recipeId);
                    image.setId(UUID.randomUUID().toString());

                    image.setUrl(s3Hanlder.uploadfile(file, image.getId(), suffix));
                    imageRepository.save(image);

                    jsonObject.addProperty("id", image.getId());
                    jsonObject.addProperty("url", image.getUrl());
                    response.setStatus(HttpServletResponse.SC_CREATED);
                }

            }
        }

        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.DELETE, produces = "application/json")
    protected String deleteImage(HttpServletRequest request, HttpServletResponse response) {

        JsonObject jsonObject = new JsonObject();

        String recipeId = request.getRequestURI().split("/")[3];
        String imageId = request.getRequestURI().split("/")[5];
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
                Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
                Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
                if (recipe == null) {
                    jsonObject.addProperty("error message", "no such recipe");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (!recipe.getAuthor_id().equals(user.getId())) {
                    jsonObject.addProperty("error message", "no recipe access");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    Optional<Image> optionalImage = imageRepository.findById(imageId);
                    Image image = optionalImage.isPresent() ? optionalImage.get() : null;
                    if (image == null) {
                        jsonObject.addProperty("error message", "no such image");
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    } else if (!image.getRecipeId().equals(recipeId)) {
                        jsonObject.addProperty("error message", "no image access");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    } else {
                        s3Hanlder.deletefile(imageId);
                        imageRepository.delete(image);
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                }
            }
        }
        return jsonObject.toString();
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.GET, produces = "application/json")
    public String getImage(HttpServletRequest request, HttpServletResponse response) {
        JsonObject jsonObject = new JsonObject();

        String recipeId = request.getRequestURI().split("/")[3];
        String imageId = request.getRequestURI().split("/")[5];

        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
        Recipe recipe = optionalRecipe.isPresent() ? optionalRecipe.get() : null;
        if (recipe == null) {
            jsonObject.addProperty("error message", "no such recipe");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            Optional<Image> optionalImage = imageRepository.findById(imageId);
            Image image = optionalImage.isPresent() ? optionalImage.get() : null;
            if (image == null) {
                jsonObject.addProperty("error message", "no such image");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else if (!image.getRecipeId().equals(recipeId)) {
                jsonObject.addProperty("error message", "no image access");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                jsonObject.addProperty("id", imageId);
                jsonObject.addProperty("url", image.getUrl());
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }

        return jsonObject.toString();
    }
}
