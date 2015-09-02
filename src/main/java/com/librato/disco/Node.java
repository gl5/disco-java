package com.librato.disco;

import java.util.Objects;

/**
 * Represents information about a particular discovered node
 */
public class Node {
    public final String host;
    public final int port;

    public Node(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(port, node.port) &&
                Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
