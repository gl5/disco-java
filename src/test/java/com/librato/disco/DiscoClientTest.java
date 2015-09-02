package com.librato.disco;

import com.google.common.base.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiscoClientTest {
    private static final Logger log = LoggerFactory.getLogger(DiscoClientTest.class);

    CuratorFramework framework;
    DiscoClient client;

    @Test
    public void testBasicListener() throws Exception {
        final String serviceName = "myservice";
        framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
        final DiscoClientFactory factory = new DiscoClientFactory(framework);
        client = factory.buildClient(serviceName);

        assertEquals(0, client.numServiceHosts());

        framework.create().withMode(CreateMode.EPHEMERAL).forPath("/services/myservice/nodes/hello:1231");
        // Give it a bit to propagate
        Thread.sleep(100);
        assertEquals(new Node("hello", 1231), client.getServiceNode().get());
        assertEquals(1, client.numServiceHosts());
    }

    @Test
    public void testGetServiceNode() throws Exception {
        framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
        final DiscoClientFactory factory = new DiscoClientFactory(framework);
        client = factory.buildClient("myservice");
        assertEquals(0, client.numServiceHosts());
        framework.create().withMode(CreateMode.EPHEMERAL).forPath("/services/myservice/nodes/hello1:1231");
        // Give it a bit to propagate
        Thread.sleep(100);
        Optional<Node> node = client.getServiceNode();
        assertTrue(node.isPresent());
        assertEquals("hello1", node.get().host);
        assertEquals(1231, node.get().port);
    }

    @Test
    public void testAllNodes() throws Exception {
        framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString("localhost:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
        final DiscoClientFactory factory = new DiscoClientFactory(framework);
        client = factory.buildClient("myservice");

        assertEquals(0, client.numServiceHosts());

        framework.create().withMode(CreateMode.EPHEMERAL).forPath("/services/myservice/nodes/hello1:1231");
        framework.create().withMode(CreateMode.EPHEMERAL).forPath("/services/myservice/nodes/hello2:1232");
        // Give it a bit to propagate
        Thread.sleep(100);

        Set<Node> nodes = new HashSet<>(client.getAllNodes());
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(new Node("hello1", 1231)));
        assertTrue(nodes.contains(new Node("hello2", 1232)));
    }

    @After
    public void tearDown() {
        if (client != null) {
            try {
                client.stop();
            } catch (IOException ex) {
                log.error("stopping DiscoService failed", ex);
            }
        }
        if (framework != null) {
            framework.close();
        }
    }
}
