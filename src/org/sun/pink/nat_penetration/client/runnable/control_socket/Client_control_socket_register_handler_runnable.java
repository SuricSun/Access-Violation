package org.sun.pink.nat_penetration.client.runnable.control_socket;

import org.sun.pink.nat_penetration.client.Client;
import org.sun.pink.nat_penetration.common.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/26
 */
public class Client_control_socket_register_handler_runnable implements Runnable {

    public Client client;
    public Simple_socket_addr server_addr;
    public String name_to_register;

    public Client_control_socket_register_handler_runnable(Client client, Simple_socket_addr server_addr, String name_to_register) {

        this.client = client;
        this.server_addr = server_addr;
        this.name_to_register = name_to_register;
    }

    @Override
    public void run() {

        // * 先建立连接
        long end_time = -1;
        SocketChannel conn = null;
        try {
            conn = SocketChannel.open();
            conn.configureBlocking(false);
            conn.connect(new InetSocketAddress(this.server_addr.ip, server_addr.port0));
            end_time = System.currentTimeMillis() + this.client.control_socket_register_wait_timeout_ms;
            do {
                if (System.currentTimeMillis() > end_time) {
                    //超时
                    System.out.println("server_socket连接到服务器超时");
                    //TODO: close a unconnected socket?
                    Socket_util.Close_socket(conn);
                    return;
                }
            } while (conn.finishConnect() == false);
        } catch (Exception e) {
            //连接失败
            Logger.Log("file", Logger.Log_type.Error, "server_socket注册异常", Logger.Stringify(e));
            return;
        }
        // * 连接成功, 发送要注册的名称
        if (Socket_util.Send_to_socket(
                conn,
                Simple_data_package.Generate_byte_buffer_by(this.name_to_register),
                this.client.control_socket_register_wait_timeout_ms
        ) == false) {
            //发送超时
            System.out.println("server_socket发送name包超时或断开连接");
            Socket_util.Close_socket(conn);
            return;
        }
        //等待4字节回馈
        ByteBuffer $4byte = ByteBuffer.allocate(4);
        try {
            end_time = System.currentTimeMillis() + this.client.control_socket_register_wait_timeout_ms;
            while ($4byte.hasRemaining()) {
                if (System.currentTimeMillis() > end_time) {
                    //超时
                    System.out.println("等待server_socket回馈超时");
                    Socket_util.Close_socket(conn);
                    return;
                }
                if (conn.read($4byte) == -1) {
                    System.out.println("server_socket断开连接");
                    Socket_util.Close_socket(conn);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("等待回馈中断开连接: " + Logger.Stringify(e));
            return;
        }
        // * 检查4字节反馈
        $4byte.flip();
        int ret = $4byte.getInt();
        if (ret != Global_error_code.SUCCESS) {
            System.out.println("server_socket注册失败, 返回值: " + ret);
            return;
        }
        // * 注册成功
        Control_socket control_socket = null;
        try {
            //注册
            control_socket = new Control_socket(conn, this.name_to_register, this.server_addr);
            control_socket.init_parser(this.client.maximum_data_packet_size);
            //注册到selector
            control_socket.conn.register(
                    this.client.unsafe_server_socket_selector_that_has_minimum_channels,
                    SelectionKey.OP_READ,
                    control_socket
            );
        } catch (Exception e) {
            //注册失败
            Logger.Log("file", Logger.Log_type.Error, "server_socket注册到selector", Logger.Stringify(e));
            Socket_util.Close_socket(conn);
            return;
        }

        System.out.println("server socket注册成功");
        control_socket.set_backup_sync_count(this.client.sync_count);
        this.client.sync_count.incr_control_socket_count();
    }
}
