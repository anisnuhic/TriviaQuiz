package servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.UserService;
import model.User;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "SignupServlet", urlPatterns = { "/signup", "/register" })
public class SignupServlet extends HttpServlet {

    private UserService userService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        userService = new UserService();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect("signupPage.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        Map<String, Object> jsonResponse = new HashMap<>();

        try {

            String username = request.getParameter("username");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");
            String firstName = request.getParameter("firstName");
            String lastName = request.getParameter("lastName");

            if (username == null || username.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Korisničko ime je obavezno!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (email == null || email.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Email adresa je obavezna!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (password == null || password.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Lozinka je obavezna!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (confirmPassword == null || !password.equals(confirmPassword)) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Lozinke se ne poklapaju!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (!isValidUsername(username)) {
                jsonResponse.put("success", false);
                jsonResponse.put("message",
                        "Korisničko ime mora imati 3-20 karaktera i može sadržavati slova, brojeve i _");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (!isValidEmail(email)) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Unesite validnu email adresu!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (!isValidPassword(password)) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Lozinka mora imati najmanje 8 karaktera, velika i mala slova, brojeve!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            User newUser = userService.registerUser(username, email, password, firstName, lastName);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Nalog je uspiješno kreiran! Provjerite email za verifikaciju.");
            jsonResponse.put("redirectUrl", "loginPage.html");
            jsonResponse.put("userId", newUser.getId());

            HttpSession session = request.getSession();
            session.setAttribute("user", newUser);
            session.setAttribute("userId", newUser.getId());
            session.setAttribute("username", newUser.getUsername());

            out.print(gson.toJson(jsonResponse));

        } catch (IllegalArgumentException e) {

            jsonResponse.put("success", false);
            jsonResponse.put("message", e.getMessage());
            out.print(gson.toJson(jsonResponse));

        } catch (Exception e) {

            jsonResponse.put("success", false);
            jsonResponse.put("message", e.getMessage());
            out.print(gson.toJson(jsonResponse));

        } finally {
            out.flush();
            out.close();
        }
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        username = username.trim();
        return username.length() >= 3 && username.length() <= 20 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);

        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);

        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUppercase && hasLowercase && hasDigit;
    }

    private String[] parseFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[] { "", "" };
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new String[] { parts[0], "" };
        } else {
            return new String[] { parts[0], parts[1] };
        }
    }
}