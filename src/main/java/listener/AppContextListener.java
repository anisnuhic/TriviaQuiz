package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import util.JPAUtil;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Aplikacija se pokretala...");
        
        try {
            JPAUtil.getEntityManager().close();
            System.out.println("JPA successfully initialized");
        } catch (Exception e) {
            System.err.println("Error initializing JPA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Aplikacija se gasi...");
        JPAUtil.close();
    }
}