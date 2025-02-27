package ai.netoai.collector;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.server.ResourceConfig;

public class AppServer {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Jersey configuration
        ResourceConfig config = new ResourceConfig();
        config.packages("ai.netoai.collector"); // Scans this package for API classes
        ServletContainer servlet = new ServletContainer(config);
        context.addServlet(new org.eclipse.jetty.servlet.ServletHolder(servlet), "/api/*");

        System.out.println("Server started at http://localhost:8080");
        server.start();
        server.join();
    }
}
