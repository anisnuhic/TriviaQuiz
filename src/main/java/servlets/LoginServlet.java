package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@WebServlet(name = "LoginServlet", urlPatterns = { "/login", "/trivia/login" })
public class LoginServlet extends HttpServlet {
    private UserService userService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        userService = new UserService();
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String usernameOrEmail = req.getParameter("usernameOrEmail");
        String password = req.getParameter("password");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (usernameOrEmail.equals("superadmin") && password.equals("Admin123")) {
            HttpSession session = req.getSession(true);
            session.setAttribute("user", "superadmin");
            session.setAttribute("userId", "superadmin");
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("isSuperadmin", true);
            response.addProperty("message", "Login successful");
            resp.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = resp.getWriter();
            out.print(gson.toJson(response));
            out.flush();
            return;
        }

        Optional<User> userOptional = userService.login(usernameOrEmail, password);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user.getUsername());
            session.setAttribute("userId", user.getId());

            JsonObject userJson = createUserJson(user);
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Login successful");
            response.add("user", userJson);

            System.out.println("Response JSON: " + gson.toJson(response));

            resp.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = resp.getWriter();
            out.print(gson.toJson(response));
            out.flush();

        } else {

            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid username/email or password");

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter out = resp.getWriter();
            out.print(gson.toJson(response));
            out.flush();
        }
    }

    private JsonObject createUserJson(User user) {
        JsonObject userJson = new JsonObject();
        userJson.addProperty("id", user.getId());
        userJson.addProperty("username", user.getUsername());
        userJson.addProperty("email", user.getEmail());
        userJson.addProperty("firstName", user.getFirstName());
        userJson.addProperty("lastName", user.getLastName());
        userJson.addProperty("profileImage", user.getProfileImage());
        userJson.addProperty("isVerified", user.getIsVerified());
        userJson.addProperty("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        userJson.addProperty("fullName", user.getFullName());

        return userJson;
    }
}