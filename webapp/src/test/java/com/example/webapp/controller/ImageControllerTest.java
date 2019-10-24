package com.example.webapp.controller;

import com.example.webapp.dao.ImageRepository;
import com.example.webapp.helpers.S3Hanlder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ImageControllerTest {

    @Autowired
    ImageController imageController;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockMultipartFile mockMultipartFile;

    @Before
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");
        mockMultipartFile = new MockMultipartFile("file", readBytesFromFile(S3Hanlder.LOCAL_DIR + "time.jpg"));
    }

    @Test
    @Transactional
    public void postImage() {

        request.setRequestURI("/v1/recipe/00b206a1-0de8-4f56-a062-65120fa14947/image");
        String result = imageController.postImage(mockMultipartFile, request, response);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(result, JsonObject.class);
        new S3Hanlder().deletefile(jsonObject.get("id").getAsString());

    }

    @Test
    @Transactional
    public void deleteImage() {
        request.setRequestURI("/v1/recipe/00b206a1-0de8-4f56-a062-65120fa14947/image/659a7c2d-f10a-4b80-91f3-36835395f303");
        imageController.deleteImage(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        new S3Hanlder().uploadfile(mockMultipartFile, "659a7c2d-f10a-4b80-91f3-36835395f303","jpg");
    }

    @Test
    public void getImage() {
        request.setRequestURI("/v1/recipe/00b206a1-0de8-4f56-a062-65120fa14947/image/659a7c2d-f10a-4b80-91f3-36835395f303");
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