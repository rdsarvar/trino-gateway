package io.trino.gateway.ha.clustermonitor;

import io.trino.gateway.ha.router.BackendHealth;
import io.trino.gateway.ha.router.RoutingManager;

@lombok.extern.slf4j.Slf4j
public class HealthCheckObserver implements TrinoClusterStatsObserver {
  private final RoutingManager routingManager;


  public HealthCheckObserver(RoutingManager routingManager) {
    this.routingManager = routingManager;
  }

  @Override
  public void observe(java.util.List<ClusterStats> clustersStats) {
    for (ClusterStats clusterStats : clustersStats) {
      routingManager.updateBackendHealth(clusterStats.getClusterId(), clusterStats.isHealthy() ? BackendHealth.HEALTHY : BackendHealth.UNHEALTHY);
    }
  }

}
