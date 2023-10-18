package org.sun.pink.nat_penetration.common;

import org.sun.pink.nat_penetration.server.Server;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/26
 */
public class Control_socket {

    public Sync_count sync_count = null;

    public SocketChannel conn = null;

    //used by server
    public String registered_name = null;
    public ByteBuffer unfinished_write_buffer = null;

    // * used by client
    public Simple_socket_addr server_socket_addr = null;

    //used by all
    public Simple_data_package_parser parser = null;
    public ByteBuffer $1byte_buffer = null;

    /**
     * used by server
     *
     * @param conn
     * @param registered_name
     */
    public Control_socket(SocketChannel conn, String registered_name) {

        this.conn = conn;
        this.registered_name = registered_name;
    }

    /**
     * used by client
     *
     * @param conn
     * @param registered_name
     * @param server_socket_addr
     */
    public Control_socket(SocketChannel conn, String registered_name, Simple_socket_addr server_socket_addr) {

        this.conn = conn;
        this.registered_name = registered_name;
        this.server_socket_addr = server_socket_addr;
    }

    /**
     * size本身的4字节不考虑在内
     *
     * @param maximum_data_package_size
     */
    public void init_parser(int maximum_data_package_size) {

        this.parser = new Simple_data_package_parser(maximum_data_package_size);
    }

    public void init_1byte_buffer() {

        this.$1byte_buffer = ByteBuffer.allocate(1);
    }

    public void set_backup_sync_count(Sync_count sync_count) {

        this.sync_count = sync_count;
    }

    public void self_cleanup() {

        Socket_util.Close_socket(this.conn);
        if (sync_count != null) {
            this.sync_count.decr_control_socket_count();
            System.out.println(this.sync_count.toString());
        }
    }
}
