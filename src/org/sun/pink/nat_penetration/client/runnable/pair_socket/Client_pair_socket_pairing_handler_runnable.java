package org.sun.pink.nat_penetration.client.runnable.pair_socket;

import org.sun.pink.nat_penetration.client.Client;
import org.sun.pink.nat_penetration.common.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/26
 */
public class Client_pair_socket_pairing_handler_runnable implements Runnable {

    public Client client;
    public Pair_socket pair;

    public Client_pair_socket_pairing_handler_runnable(Client client, Pair_socket pair) {

        this.client = client;
        this.pair = pair;
    }

    @Override
    public void run() {

        //开始配对
        //先与路由方建立连接
        long end_time = -1;
        SocketChannel outer_conn, inner_conn;
        try {
            outer_conn = SocketChannel.open();
            outer_conn.configureBlocking(false);
            outer_conn.connect(new InetSocketAddress(this.pair.outer_socket_addr.ip, this.pair.outer_socket_addr.port0));
            end_time = System.currentTimeMillis() + this.client.socket_pair_pairing_wait_timeout_ms;
            do {
                if (System.currentTimeMillis() > end_time) {
                    //超时，取消配对
                    System.out.println("与outer socket建立连接超时");
                    Socket_util.Close_socket(outer_conn);
                    return;
                }
            } while (!outer_conn.finishConnect());
        } catch (Exception e) {
            System.out.println("与outer socket建立连接失败");
            System.out.println(Logger.Stringify(e));
            return;
        }

        //与路由方连接成功
        // * 现在与服务器建立配对
        try {
            inner_conn = SocketChannel.open();
            inner_conn.configureBlocking(false);
            //注意这里是port1
            inner_conn.connect(new InetSocketAddress(this.pair.server_to_connect_to.ip, this.pair.server_to_connect_to.port1));
            do {
                if (System.currentTimeMillis() > end_time) {
                    //超时，取消配对
                    System.out.println("与inner socket建立连接超时");
                    Socket_util.Close_socket(outer_conn);
                    Socket_util.Close_socket(inner_conn);
                    return;
                }
            } while (!inner_conn.finishConnect());
            //发送对方要配对的name
            ByteBuffer buffer = Simple_data_package.Generate_byte_buffer_by(this.pair.inner_socket_id);
            end_time = System.currentTimeMillis() + this.client.socket_pair_pairing_wait_timeout_ms;
            if (Socket_util.Send_to_socket(inner_conn, buffer, this.client.socket_pair_pairing_wait_timeout_ms) == false) {
                System.out.println("发送配对包超时");
                Socket_util.Close_socket(outer_conn);
                Socket_util.Close_socket(inner_conn);
                return;
            }
            // * 发送成功，等待4字节回馈
            ByteBuffer $4byte = ByteBuffer.allocate(4);
            try {
                end_time = System.currentTimeMillis() + this.client.control_socket_register_wait_timeout_ms;
                while ($4byte.hasRemaining()) {
                    if (System.currentTimeMillis() > end_time) {
                        //超时
                        System.out.println("等待inner socket回馈超时");
                        Socket_util.Close_socket(outer_conn);
                        Socket_util.Close_socket(inner_conn);
                        return;
                    }
                    if (inner_conn.read($4byte) == -1) {
                        System.out.println("inner socket断开连接");
                        Socket_util.Close_socket(inner_conn);
                        return;
                    }
                }
            } catch (Exception e) {
                Socket_util.Close_socket(outer_conn);
                Socket_util.Close_socket(inner_conn);
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
            // * 配对成功
            System.out.println("配对成功");
            this.pair.outer_socket_id = Socket_util.Get_addr_string(outer_conn);
            this.pair.outer_socket = outer_conn;
            this.pair.unsafe_inner_socket = inner_conn;
            this.pair.init_buffer(this.client.socket_pair_byte_buffer_size_byte);
            //注册到selector
            Selector sel = this.client.unsafe_socket_pair_selector_that_has_minimum_channels;
            try {
                this.pair.outer_socket.register(
                        sel,
                        SelectionKey.OP_READ,
                        this.pair
                );
                this.pair.unsafe_inner_socket.register(
                        sel,
                        SelectionKey.OP_READ,
                        this.pair
                );
            } catch (Exception e) {
                this.pair.self_cleanup();
                Logger.Log(
                        "file",
                        Logger.Log_type.Warning,
                        "outer_socket配对失败, 无法注册到selector",
                        Logger.Stringify(e)
                );
                return;
            }
        } catch (Exception e) {
            this.pair.self_cleanup();
            Logger.Log("file", Logger.Log_type.Error, "inner_socket配对失败", Logger.Stringify(e));
            return;
        }

        // * 配对成功
        this.pair.set_backup_sync_count(this.client.sync_count);
        this.client.sync_count.incr_pair_socket_count();
    }
}
