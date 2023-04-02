package org.web3j.spring.autoconfigure;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.web3j.protocol.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

import static org.junit.jupiter.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThat;

class Web3jAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    void testEmptyClientAddress() throws Exception {
        verifyHttpConnection("", HttpService.DEFAULT_URL, HttpService.class);
    }

    @Test
    void testHttpClient() throws Exception {
        verifyHttpConnection(
                "https://localhost:12345", HttpService.class);
    }

    @Test
    void testUnixIpcClient() throws IOException {
        Path path = Files.createTempFile("unix", "ipc");
        path.toFile().deleteOnExit();

        load(EmptyConfiguration.class, "web3j.client-address=" + path);
    }

    @Test
    void testWindowsIpcClient() throws IOException {
        // Windows uses a RandomAccessFile to access the named pipe, hence we can initialise
        // the WindowsIPCService in web3j
        Path path = Files.createTempFile("windows", "ipc");
        path.toFile().deleteOnExit();

        System.setProperty("os.name", "windows");
        load(EmptyConfiguration.class, "web3j.client-address=" + path);
    }

    @Test
    void testAdminClient() {
        load(EmptyConfiguration.class, "web3j.client-address=", "web3j.admin-client=true");

        this.context.getBean(Admin.class);
        try {
            this.context.getBean(Web3j.class);
            fail();
        } catch (NoSuchBeanDefinitionException e) {
        }
    }

    @Test
    void testNoAdminClient() {
        load(EmptyConfiguration.class, "web3j.client-address=");

        this.context.getBean(Web3j.class);
        try {
            this.context.getBean(Admin.class);
            fail();
        } catch (NoSuchBeanDefinitionException e) {
        }
    }


    @Test
    void testHealthCheckIndicatorDown() {
        load(EmptyConfiguration.class, "web3j.client-address=");

        HealthIndicator web3jHealthIndicator = this.context.getBean(HealthIndicator.class);
        Health health = web3jHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error").toString()).startsWith("java.net.ConnectException: Failed to connect to localhost/");
    }

    private void verifyHttpConnection(
            String clientAddress, Class<? extends Service> cls) throws Exception {
        verifyHttpConnection(clientAddress, clientAddress, cls);
    }

    private void verifyHttpConnection(
            String clientAddress, String expectedClientAddress, Class<? extends Service> cls)
            throws Exception {
        load(EmptyConfiguration.class, "web3j.client-address=" + clientAddress);
        Web3j web3j = this.context.getBean(Web3j.class);

        Field web3jServiceField = JsonRpc2_0Web3j.class.getDeclaredField("web3jService");
        web3jServiceField.setAccessible(true);
        Web3jService web3jService = (Web3jService) web3jServiceField.get(web3j);

        assertThat(cls.isInstance(web3jService)).isTrue();

        Field urlField = HttpService.class.getDeclaredField("url");
        urlField.setAccessible(true);
        String url = (String) urlField.get(web3jService);

        assertThat(url).isEqualTo(expectedClientAddress);
    }

    @Configuration
    static class EmptyConfiguration {
    }

    private void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        Arrays.stream(environment).forEach(env -> EnvironmentTestUtils.addEnvironment(applicationContext, env));
        applicationContext.register(config);
        applicationContext.register(Web3jAutoConfiguration.class);
        applicationContext.refresh();
        this.context = applicationContext;
    }

    private static class EnvironmentTestUtils {
        public static void addEnvironment(ConfigurableApplicationContext context, String pair) {
            MutablePropertySources sources = context.getEnvironment().getPropertySources();
            String[] split = pair.split("=");
            if (split.length == 2) {
                String key = split[0];
                String value = split[1];
                sources.addFirst(new MapPropertySource("test", Collections.singletonMap(key, value)));
            }
        }
    }
}
