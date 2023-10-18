package org.sun.pink.nat_penetration.common;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Pair_socket {

    public Sync_count sync_count = null;

    public String outer_socket_id = null;
    public String inner_socket_id = null;

    public SocketChannel outer_socket = null;
    public volatile SocketChannel unsafe_inner_socket = null;

    public ByteBuffer outer2inner_buffer = null;
    public ByteBuffer inner2outer_buffer = null;

    //used by server
    public String router_client_socket_friendly_name_to_route_to = null;
    //used by client
    public Simple_socket_addr outer_socket_addr = null;
    public Simple_socket_addr server_to_connect_to = null;

    public void set_backup_sync_count(Sync_count sync_count) {

        this.sync_count = sync_count;
    }

    public void init_buffer(int size) {

        this.outer2inner_buffer = ByteBuffer.allocate(size);
        this.inner2outer_buffer = ByteBuffer.allocate(size);
    }

    public void self_cleanup() {

        Socket_util.Close_socket(this.unsafe_inner_socket);
        Socket_util.Close_socket(this.outer_socket);
        if (sync_count != null) {
            this.sync_count.decr_pair_socket_count();
            System.out.println(this.sync_count.toString());
        }
    }
}
