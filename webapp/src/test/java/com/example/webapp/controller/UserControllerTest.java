package com.example.webapp.controller;

import com.example.webapp.entities.User;
import com.google.gson.Gson;
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
public class UserControllerTest {

    @Autowired
    UserController userController;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    Gson gson;

    @Before
    @Transactional
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");

        gson = new Gson();

        //prepare database
        String test = "{'username':'yuan@husky.edu','password' : '123abcABC','firstname' : 'Ke','lastname' : 'Yuan'}";
        User user = gson.fromJson(test, User.class);
        userController.createNewUser(user, request, response);
    }

    @Test
    @Transactional
    public void createNewUserTest() {

        String test = "{'username':'yuan.ke@husky.edu','password' : '123ABCabc','firstname' : 'Ke','lastname' : 'Yuan'}";

        User user = gson.fromJson(test, User.class);

        String result = userController.createNewUser(user, request, response);

        assertEquals(true, result != null);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
    }

    @Test
    @Transactional
    public void getUserInformationTest() {

        String result = userController.getUserInformation(request, response);

        assertEquals(true, result != null);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @Transactional
    public void updateupdateUserInformation() {

        String test = "{'password' : '123ABCabc','firstname' : 'Keke','lastname' : 'Yuan'}";

        User user = gson.fromJson(test, User.class);

        String result = userController.updateUserInformation(user, request, response);

        assertEquals(true, result != null);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

}