package com.example.webapp.service;

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

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    private MockHttpServletRequest request;
    private User user;
    private Gson gson;

    @Before
    public void setUp() {
        String username = "yuan@husky.edu";
        String password = "123abcABC";
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setCharacterEncoding("UTF-8");

        gson = new Gson();
    }

    @Test
    @Transactional
    public void createNewUserTest() {

        String test = "{'username':'yuan.ke@husky.edu','password' : '123ABCabc','firstname' : 'Ke','lastname' : 'Yuan'}";

        User user = gson.fromJson(test, User.class);

        String result = userService.createNewUser(user);
        assertEquals(true, result != null);

        JsonObject jsonObject = gson.fromJson(result, JsonObject.class);

        assertEquals(user.getUsername(), jsonObject.get("email_address").getAsString());
        assertEquals(user.getFirstname(), jsonObject.get("first_name").getAsString());
        assertEquals(user.getLastname(), jsonObject.get("last_name").getAsString());

    }

    @Test
    @Transactional
    public void getUserInformationTest() {

        String test = "{'username':'yuan@husky.edu','password' : '123ABCabc','firstname' : 'Ke','lastname' : 'Yuan'}";

        User user = gson.fromJson(test, User.class);

        String result = userService.getUserInformation(request);

        assertEquals(true, result != null);

        JsonObject jsonObject = gson.fromJson(result, JsonObject.class);
        assertEquals(user.getUsername(), jsonObject.get("email_address").getAsString());
        assertEquals(user.getFirstname(), jsonObject.get("first_name").getAsString());
        assertEquals(user.getLastname(), jsonObject.get("last_name").getAsString());
    }

    @Test
    @Transactional
    public void updateupdateUserInformation() {

        String test = "{'password' : '123ABCabc','firstname' : 'Keke','lastname' : 'Yuan'}";

        User user = gson.fromJson(test, User.class);

        String result = userService.updateUserInformation(user, request);

        assertEquals(true, result != null);

        JsonObject jsonObject = gson.fromJson(result, JsonObject.class);
        assertEquals(user.getFirstname(), jsonObject.get("first_name").getAsString());
        assertEquals(user.getLastname(), jsonObject.get("last_name").getAsString());
    }

}