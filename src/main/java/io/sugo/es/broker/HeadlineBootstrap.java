package io.sugo.es.broker;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import io.sugo.utils.LogUtil;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class HeadlineBootstrap {

  private static final AtomicInteger activeConnections = new AtomicInteger();
  public static final CountDownLatch latch = new CountDownLatch(1);

  public static void main(String[] args) throws Exception {
    Server server = null;
    try {
      SystemConfig.tryLock(SystemConfig.getLockFile());
      LogUtil.info("get lock file:" + SystemConfig.getLockFile());
      SystemConfig.parseArgs(args);

      String ip = SystemConfig.getString(SystemConfig.LISTEN_IP, "0.0.0.0");
      int port = SystemConfig.getInt(SystemConfig.LISTEN_PORT);
      server = makeJettyServer(ip, port);
      initialize(server);
      server.start();
      System.out.println(String.format("listen: [%s:%d] ...", ip, port));
      latch.await();
    } finally {
      if (server != null) {
        server.stop();
      }
      System.out.println("server stopped!");
    }
  }

  static Server makeJettyServer(String ip, int port) {
    final Server server = new Server(new InetSocketAddress(ip, port));

    // Without this bean set, the default ScheduledExecutorScheduler runs as non-daemon, causing lifecycle hooks to fail
    // to fire on main exit. Related bug: https://github.com/druid-io/druid/pull/1627
    server.addBean(new ScheduledExecutorScheduler("JettyScheduler", true), true);

    ServerConnector connector = new ServerConnector(server);
    connector.setHost(ip);
    connector.setPort(port);
    connector.setIdleTimeout(600000);
    // workaround suggested in -
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=435322#c66 for jetty half open connection issues during failovers
    connector.setAcceptorPriorityDelta(-1);

    List<ConnectionFactory> monitoredConnFactories = new ArrayList<>();
    for (ConnectionFactory cf : connector.getConnectionFactories()) {
      monitoredConnFactories.add(new JettyMonitoringConnectionFactory(cf, activeConnections));
    }
    connector.setConnectionFactories(monitoredConnFactories);

    server.setConnectors(new Connector[]{connector});

    return server;
  }

  static void initialize(Server server) {
    HandlerList handlerList = new HandlerList();

    final ServletContextHandler htmlHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    htmlHandler.setContextPath("/");
    htmlHandler.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
    htmlHandler.setInitParameter("org.eclipse.jetty.servlet.Default.redirectWelcome", "true");
    htmlHandler.setWelcomeFiles(new String[]{"index.html"});

    ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);

    htmlHandler.addServlet(holderPwd, "/*");
    htmlHandler.setBaseResource(
        new ResourceCollection(
            new String[]{
                HeadlineBootstrap.class.getClassLoader().getResource("static").toExternalForm(),
                HeadlineBootstrap.class.getClassLoader().getResource("indexer_static").toExternalForm()
            }
        )
    );
    ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
    Map<String, String> params = new HashMap<>();
    params.put("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
    params.put("com.sun.jersey.config.property.packages", "io.sugo.es.broker.resource");
    params.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
//    SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS(false);
    servletHolder.setInitParameters(params);
    htmlHandler.addServlet(servletHolder, "/api/*");
//    htmlHandler.addServlet(servletHolder, "/druid/coordinator/v1/*");
    handlerList.addHandler(htmlHandler);

    server.setHandler(handlerList);
  }

}
