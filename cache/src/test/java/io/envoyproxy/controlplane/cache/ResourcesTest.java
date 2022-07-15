package io.envoyproxy.controlplane.cache;

import static io.envoyproxy.envoy.config.core.v3.ApiVersion.V3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.type.Color;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.Secret;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

public class ResourcesTest {

  private static final boolean ADS = ThreadLocalRandom.current().nextBoolean();
  private static final String CLUSTER_NAME = "v3cluster";
  private static final String LISTENER_NAME = "v3listener";
  private static final String ROUTE_NAME = "v3route";
  private static final String SECRET_NAME = "v3secret";

  private static final int ENDPOINT_PORT = ThreadLocalRandom.current().nextInt(10000, 20000);
  private static final int LISTENER_PORT = ThreadLocalRandom.current().nextInt(20000, 30000);

  private static final Cluster CLUSTER = TestResources.createCluster(CLUSTER_NAME);
  private static final ClusterLoadAssignment ENDPOINT =
      TestResources.createEndpoint(CLUSTER_NAME, ENDPOINT_PORT);
  private static final Listener LISTENER =
      TestResources.createListener(ADS, V3, V3, LISTENER_NAME, LISTENER_PORT, ROUTE_NAME);
  private static final RouteConfiguration ROUTE =
      TestResources.createRoute(ROUTE_NAME, CLUSTER_NAME);
  private static final Secret SECRET = TestResources.createSecret(SECRET_NAME);

  @Test
  public void getResourceNameReturnsExpectedNameForValidResourceMessage() {
    Map<Message, String> cases =
        ImmutableMap.of(
            CLUSTER, CLUSTER_NAME,
            ENDPOINT, CLUSTER_NAME,
            LISTENER, LISTENER_NAME,
            ROUTE, ROUTE_NAME,
            SECRET, SECRET_NAME);

    cases.forEach(
        (resource, expectedName) ->
            assertThat(Resources.getResourceName(resource)).isEqualTo(expectedName));
  }

  @Test
  public void getResourceNameReturnsEmptyStringForNonResourceMessage() {
    Message message = Color.newBuilder().build();

    assertThat(Resources.getResourceName(message)).isEmpty();
  }

  @Test
  public void getResourceNameAnyThrowsOnBadClass() {
    assertThatThrownBy(() -> Resources.getResourceName(Any.newBuilder().setTypeUrl("garbage").build()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("cannot unpack");
  }

  @Test
  public void getResourceReferencesReturnsExpectedReferencesForValidResourceMessages() {
    String clusterServiceName = "clusterWithServiceName0";
    Cluster clusterWithServiceName =
        Cluster.newBuilder()
            .setName(CLUSTER_NAME)
            .setEdsClusterConfig(
                Cluster.EdsClusterConfig.newBuilder().setServiceName(clusterServiceName))
            .setType(Cluster.DiscoveryType.EDS)
            .build();

    Map<Collection<Message>, Set<String>> cases =
        ImmutableMap.<Collection<Message>, Set<String>>builder()
            .put(ImmutableList.of(CLUSTER), ImmutableSet.of(CLUSTER_NAME))
            .put(ImmutableList.of(clusterWithServiceName), ImmutableSet.of(clusterServiceName))
            .put(ImmutableList.of(ENDPOINT), ImmutableSet.of())
            .put(ImmutableList.of(LISTENER), ImmutableSet.of(ROUTE_NAME))
            .put(ImmutableList.of(ROUTE), ImmutableSet.of())
            .put(
                ImmutableList.of(CLUSTER, ENDPOINT, LISTENER, ROUTE),
                ImmutableSet.of(CLUSTER_NAME, ROUTE_NAME))
            .build();

    cases.forEach(
        (resources, refs) ->
            assertThat(Resources.getResourceReferences(resources)).containsExactlyElementsOf(refs));
  }
}
