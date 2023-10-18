package org.sun.pink.nat_penetration.server.runnable.control_socket;

import org.sun.pink.nat_penetration.common.*;
import org.sun.pink.nat_penetration.server.Server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/26
 */
public class Server_control_socket_register_handler_runnable implements Runnable {

    public Server server;
    public SocketChannel conn;

    public Server_control_socket_register_handler_runnable(Server server, SocketChannel conn) {

        this.server = server;
        this.conn = conn;
    }

    @Override
    public void run() {

        //等待对方发送注册包
        Control_socket control_socket = null;
        Simple_data_package packet = null;
        Simple_data_package_parser parser = new Simple_data_package_parser(this.server.maximum_data_packet_size);
        long end_time = System.currentTimeMillis() + this.server.control_socket_register_wait_timeout_ms;
        System.out.println("等待对方发送注册包");
        try {
            //开始配对
            this.conn.configureBlocking(false);
            do {
                if (System.currentTimeMillis() > end_time) {
                    //连接失败，超时
                    System.out.println("等待router_client发送name超时");
                    Socket_util.Close_socket(this.conn);
                    return;
                }
                //解析
                packet = parser.parse(this.conn);
            } while (packet == null);
            //解析出注册包
            System.out.println("解析出注册包, 尝试注册");
            //尝试注册
            synchronized (this.server.control_socket_send_queue_map) {
                ByteBuffer send_back_buffer  = ByteBuffer.allocate(Integer.BYTES);
                if (this.server.control_socket_send_queue_map.containsKey(packet.str_for_whatever_use)) {
                    //注册失败
                    System.out.println("注册失败, 已存在");
                    send_back_buffer.clear();
                    send_back_buffer.putInt(Global_error_code.DUPLICATED_NAME);
                    send_back_buffer.flip();
                    Socket_util.Send_to_socket(this.conn, send_back_buffer, this.server.control_socket_register_wait_timeout_ms);
                    Socket_util.Close_socket(this.conn);
                    return;
                }
                //可以注册
                send_back_buffer.clear();
                send_back_buffer.putInt(Global_error_code.SUCCESS);
                send_back_buffer.flip();
                if (Socket_util.Send_to_socket(
                        this.conn,
                        send_back_buffer,
                        this.server.control_socket_register_wait_timeout_ms
                ) == false) {
                    //发送反馈超时
                    System.out.println("发送反馈超时");
                    Socket_util.Close_socket(this.conn);
                    return;
                }
                //注册
                System.out.println("注册成功");
                control_socket = new Control_socket(this.conn, packet.str_for_whatever_use);
                this.server.control_socket_send_queue_map.put(
                        packet.str_for_whatever_use,
                        new Thread_safe_Fixed_capacity_Queue<>(
                                this.server.send_queue_size,
                                control_socket
                        )
                );
            }
            //注册到selector
            control_socket.init_1byte_buffer();
            this.conn.register(
                    this.server.unsafe_router_client_selector_that_has_minimum_channels,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                    control_socket
            );
        } catch (Exception e) {
            //注册失败
            Logger.Log("file", Logger.Log_type.Error, "router_client注册失败",Logger.Stringify(e));
            Socket_util.Close_socket(this.conn);
            return;
        }

        // * 注册成功
        control_socket.set_backup_sync_count(this.server.sync_count);
        this.server.sync_count.incr_control_socket_count();
    }
}
