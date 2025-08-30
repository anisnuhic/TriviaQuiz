package filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter("/admin/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig conf) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        HttpSession session = ((HttpServletRequest) req ).getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            chain.doFilter(req, resp);
        } else {
            ((HttpServletResponse) resp).sendRedirect("/trivia/loginPage.html?error=notAuth");
        }

    }

    @Override
    public void destroy() {

    }

    
}