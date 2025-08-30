package servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet("/admin/superadmin/users")
public class UsersServlet extends HttpServlet {

    private UserService userService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        userService = new UserService();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            sendJsonResponse(response, "{\"error\": \"Access denied. Superadmin privileges required.\"}");
            return;
        }

        try {
            List<User> users = userService.findAllUsers();
            List<UserResponse> userResponses = new ArrayList<>();

            for (User user : users) {
                userResponses.add(new UserResponse(user));
            }

            response.setStatus(HttpServletResponse.SC_OK);
            sendJsonResponse(response, gson.toJson(userResponses));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJsonResponse(response, "{\"error\": \"Failed to retrieve users: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            sendJsonResponse(response, "{\"error\": \"Access denied. Superadmin privileges required.\"}");
            return;
        }

        try {
            String jsonBody = getRequestBody(request);
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();

            if (!jsonObject.has("id")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJsonResponse(response, "{\"error\": \"User ID is required for update\"}");
                return;
            }

            Long userId = jsonObject.get("id").getAsLong();
            Optional<User> existingUserOpt = userService.findById(userId);

            if (existingUserOpt.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                sendJsonResponse(response, "{\"error\": \"User not found\"}");
                return;
            }

            User existingUser = existingUserOpt.get();

            if (jsonObject.has("username")) {
                existingUser.setUsername(jsonObject.get("username").getAsString());
            }
            if (jsonObject.has("email")) {
                existingUser.setEmail(jsonObject.get("email").getAsString());
            }
            if (jsonObject.has("firstName")) {
                existingUser.setFirstName(jsonObject.get("firstName").getAsString());
            }
            if (jsonObject.has("lastName")) {
                existingUser.setLastName(jsonObject.get("lastName").getAsString());
            }
            if (jsonObject.has("profileImage")) {
                existingUser.setProfileImage(jsonObject.get("profileImage").getAsString());
            }
            if (jsonObject.has("isVerified")) {
                existingUser.setIsVerified(jsonObject.get("isVerified").getAsBoolean());
            }

            User updatedUser = userService.updateUser(existingUser);
            UserResponse userResponse = new UserResponse(updatedUser);

            response.setStatus(HttpServletResponse.SC_OK);
            sendJsonResponse(response, gson.toJson(userResponse));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJsonResponse(response, "{\"error\": \"Failed to update user: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            sendJsonResponse(response, "{\"error\": \"Access denied. Superadmin privileges required.\"}");
            return;
        }

        String userIdParam = request.getParameter("id");

        if (userIdParam == null || userIdParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, "{\"error\": \"User ID parameter is required\"}");
            return;
        }

        try {
            Long userId = Long.parseLong(userIdParam);

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                sendJsonResponse(response, "{\"error\": \"User not found\"}");
                return;
            }

            userService.deleteUser(userId);

            response.setStatus(HttpServletResponse.SC_OK);
            sendJsonResponse(response, "{\"message\": \"User deleted successfully\"}");

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, "{\"error\": \"Invalid user ID format\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJsonResponse(response, "{\"error\": \"Failed to delete user: " + e.getMessage() + "\"}");
        }
    }

    private boolean isAuthorized(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String userRole = (String) session.getAttribute("user");
        return "superadmin".equals(userRole);
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void sendJsonResponse(HttpServletResponse response, String jsonString) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(jsonString);
            out.flush();
        }
    }

    private static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String profileImage;
        private Boolean isVerified;
        private String createdAt;
        private String updatedAt;
        private String fullName;

        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.profileImage = user.getProfileImage();
            this.isVerified = user.getIsVerified();
            this.fullName = user.getFullName();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : null;
            this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().format(formatter) : null;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public Boolean getIsVerified() {
            return isVerified;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public String getFullName() {
            return fullName;
        }
    }
}