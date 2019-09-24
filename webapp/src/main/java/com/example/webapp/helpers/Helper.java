package com.example.webapp.helpers;

import com.example.webapp.dao.UserRepository;
import com.example.webapp.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class Helper {

    @Autowired
    private UserRepository userRepository;

    public User validateUser(String header) {

        String basicAuthEncoded = header.substring(6);
        String basicAuthAsString = new String(Base64.getDecoder().decode(basicAuthEncoded.getBytes()));
        final String[] credentialValues = basicAuthAsString.split(":", 2);

        String username = credentialValues[0];
        String password = credentialValues[1];

        User user = userRepository.findByUsername(username).get(0);

        if (user != null)
            if (BCrypt.checkpw(password, user.getPassword()))
                return user;
        return null;

    }
}
