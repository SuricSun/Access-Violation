package org.sun.pink.nat_penetration.server.runnable.outer_socket;

import org.sun.pink.nat_penetration.common.*;
import org.sun.pink.nat_penetration.server.Server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server_outer_proxy_socket_pairing_handler_runnable implements Runnable {

    public Server server;
    public Pair_socket pair_socket;

    public Server_outer_proxy_socket_pairing_handler_runnable(Server server, Pair_socket pair_socket) {

        this.server = server;
        this.pair_socket = pair_socket;
    }

    @Override
    public void run() {

        // * 发送连接请求包
        Thread_safe_Fixed_capacity_Queue<ByteBuffer> q =
                this.server.control_socket_send_queue_map.get(pair_socket.router_client_socket_friendly_name_to_route_to);
        if (q == null) {
            //router client下线
            //结束连接
            System.out.println("没有此名称的router client在线");
            this.pair_socket.self_cleanup();
            return;
        }
        //发送
        boolean b = q.put(
                Simple_data_package.Generate_byte_buffer_by(this.pair_socket.outer_socket_id),
                this.server.socket_pair_pairing_wait_timeout_ms
        );
        if (b == false) {
            //发送失败
            System.out.println("发送到q超时");
            this.pair_socket.self_cleanup();
            return;
        }
        //发送成功
        // * 循环等待连接建立
        long end_time = System.currentTimeMillis() + this.server.socket_pair_pairing_wait_timeout_ms;
        //连接建立
        do {
            if (System.currentTimeMillis() > end_time) {
                //连接失败，超时
                System.out.println("等待连接建立超时");
                this.pair_socket.self_cleanup();
                return;
            }
        } while (this.pair_socket.unsafe_inner_socket == null);
        System.out.println("unsafe_inner_socket已经配对");
        //建立成功，初始化
        this.pair_socket.inner_socket_id = Socket_util.Get_addr_string(this.pair_socket.unsafe_inner_socket);
        this.pair_socket.init_buffer(this.server.socket_pair_byte_buffer_size_byte);
        //并注册到selector
        Selector sel = this.server.unsafe_socket_pair_selector_that_has_minimum_channels;
        try {
            this.pair_socket.outer_socket.configureBlocking(false);
            this.pair_socket.outer_socket.register(
                    sel,
                    SelectionKey.OP_READ,
                    this.pair_socket
            );
            this.pair_socket.unsafe_inner_socket.register(
                    sel,
                    SelectionKey.OP_READ,
                    this.pair_socket
            );
        } catch (Exception e) {
            this.pair_socket.self_cleanup();
            Logger.Log(
                    "file",
                    Logger.Log_type.Warning,
                    "outer_socket配对失败, 无法注册到selector",
                    Logger.Stringify(e)
            );
            return;
        }

        // * 注册成功
        this.pair_socket.set_backup_sync_count(this.server.sync_count);
        this.server.sync_count.incr_pair_socket_count();
    }
}
