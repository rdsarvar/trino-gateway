package io.trino.gateway.ha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.inject.Injector;
import io.trino.gateway.baseapp.BaseApp;
import io.trino.gateway.ha.config.ProxyBackendConfiguration;
import io.trino.gateway.ha.router.BackendHealth;
import io.trino.gateway.ha.router.RoutingManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import java.lang.reflect.Field;

@TestInstance(Lifecycle.PER_CLASS)
public class TestGatewayHaSingleBackend {
  public static final String EXPECTED_RESPONSE = "{\"id\":\"testId\"}";
  int backendPort = 20000 + (int) (Math.random() * 1000);
  int routerPort = 21000 + (int) (Math.random() * 1000);

  private WireMockServer backend =
      new WireMockServer(WireMockConfiguration.options().port(backendPort));
  private final OkHttpClient httpClient = new OkHttpClient();

  @BeforeAll
  public void setup() throws Exception {
    HaGatewayTestUtils.prepareMockBackend(backend, "/v1/statement", EXPECTED_RESPONSE);

    // seed database
    HaGatewayTestUtils.TestConfig testConfig =
        HaGatewayTestUtils.buildGatewayConfigAndSeedDb(routerPort, "test-config-template.yml");
    // Start Gateway
    String[] args = {"server", testConfig.getConfigFilePath()};
    HaGatewayLauncher haGatewayLauncher = new HaGatewayLauncher("io.trino");
    haGatewayLauncher.run(args);
    // Now populate the backend
    HaGatewayTestUtils.setUpBackend(
            "trino1", "http://localhost:" + backendPort, "externalUrl", true, "adhoc", routerPort);

    // When a backend becomes active for the first time OR is transitioned from inactive to active it will not yet be
    // treated as healthy. Without a real health check happening we will extract the RoutingManager and set them to healthy manually
    Field privateInjectorField = BaseApp.class.
            getDeclaredField("injector");
    privateInjectorField.setAccessible(true);

    Injector injector = (Injector) privateInjectorField.get(haGatewayLauncher);
    injector.getInstance(RoutingManager.class).updateBackendHealth("trino1", BackendHealth.HEALTHY);
  }

  @Test
  public void testRequestDelivery() throws Exception {
    RequestBody requestBody =
        RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "SELECT 1");
    Request request =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .build();
    Response response = httpClient.newCall(request).execute();
    assertEquals(EXPECTED_RESPONSE, response.body().string());
  }

  @Test
  public void testBackendConfiguration() throws Exception {
    Request request = new Request.Builder()
            .url("http://localhost:" + routerPort + "/entity/GATEWAY_BACKEND")
            .method("GET", null)
            .build();
    Response response = httpClient.newCall(request).execute();

    final ObjectMapper objectMapper = new ObjectMapper();
    ProxyBackendConfiguration[] backendConfiguration =
            objectMapper.readValue(response.body().string(), ProxyBackendConfiguration[].class);

    assertNotNull(backendConfiguration);
    assertEquals(1, backendConfiguration.length);
    assertTrue(backendConfiguration[0].isActive());
    assertEquals("adhoc", backendConfiguration[0].getRoutingGroup());
    assertEquals("externalUrl", backendConfiguration[0].getExternalUrl());
  }

  @AfterAll
  public void cleanup() {
    backend.stop();
  }
}
