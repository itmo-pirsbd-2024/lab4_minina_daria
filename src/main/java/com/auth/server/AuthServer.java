package com.auth.server;

import com.auth.server.dto.AuthRequest;
import com.auth.server.models.User;
import com.auth.server.repositories.UserRepository;
import com.auth.server.utils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;

import static spark.Spark.*;

public class AuthServer {
    private static final Logger logger = LoggerFactory.getLogger(AuthServer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final UserRepository userRepository = new UserRepository();

    public static void main(String[] args) {
        port(8080);
        enableCORS();
        setupRoutes();
    }

    private static void setupRoutes() {
        before((req, res) -> res.type("application/json"));

        post("/register", AuthServer::handleRegister);
        post("/login", AuthServer::handleLogin);
        post("/refresh", AuthServer::handleRefresh);
        get("/validate", AuthServer::validateToken);
    }

    private static Object handleRegister(Request req, Response res) {
        try {
            AuthRequest authRequest = mapper.readValue(req.body(), AuthRequest.class);

            if (!Validator.isValidCredentials(authRequest)) {
                res.status(400);
                return "{\"error\": \"Invalid credentials format\"}";
            }

            if (userRepository.findByUsername(authRequest.getUsername()).isPresent()) {
                res.status(409);
                return "{\"error\": \"Username already exists\"}";
            }

            String hashedPassword = BCrypt.hashpw(authRequest.getPassword(), BCrypt.gensalt());
            User newUser = new User(authRequest.getUsername(), hashedPassword, "USER");
            userRepository.save(newUser);

            res.status(201);
            return "{\"message\": \"User registered successfully\"}";

        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage());
            res.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    private static Object handleLogin(Request req, Response res) {
        try {
            AuthRequest authRequest = mapper.readValue(req.body(), AuthRequest.class);
            Optional<User> userOpt = userRepository.findByUsername(authRequest.getUsername());

            if (userOpt.isEmpty()) {
                res.status(401);
                return "{\"error\": \"Invalid credentials\"}";
            }

            User user = userOpt.get();
            if (user.isLocked()) {
                res.status(403);
                return "{\"error\": \"Account locked\"}";
            }

            if (!BCrypt.checkpw(authRequest.getPassword(), user.getPasswordHash())) {
                userRepository.recordLoginAttempt(user.getUsername(), false);
                res.status(401);
                return "{\"error\": \"Invalid credentials\"}";
            }

            userRepository.recordLoginAttempt(user.getUsername(), true);
            String accessToken = JwtUtil.generateAccessToken(user.getUsername(), user.getRole());
            String refreshToken = JwtUtil.generateRefreshToken(user.getUsername());

            return String.format("{\"access_token\": \"%s\", \"refresh_token\": \"%s\"}",
                    accessToken, refreshToken);

        } catch (Exception e) {
            logger.error("Login error: {}", e.getMessage());
            res.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    private static Object handleRefresh(Request req, Response res) {
        String refreshToken = req.headers("Authorization");
        if (refreshToken == null || !JwtUtil.validateToken(refreshToken, "REFRESH")) {
            res.status(401);
            return "{\"error\": \"Invalid refresh token\"}";
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(new SecretKeySpec(Config.get("jwt.secret").getBytes(), "HS256"))
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String newAccessToken = JwtUtil.generateAccessToken(claims.getSubject(),
                    claims.get("role", String.class));
            return "{\"access_token\": \"" + newAccessToken + "\"}";

        } catch (Exception e) {
            logger.error("Refresh error: {}", e.getMessage());
            res.status(401);
            return "{\"error\": \"Invalid token\"}";
        }
    }

    private static Object validateToken(Request req, Response res) {
        String token = req.headers("Authorization");
        if (token != null && JwtUtil.validateToken(token, "USER")) {
            return "{\"valid\": true}";
        }
        res.status(401);
        return "{\"valid\": false}";
    }

    private static void enableCORS() {
        options("/*", (req, res) -> "");
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });
    }
}