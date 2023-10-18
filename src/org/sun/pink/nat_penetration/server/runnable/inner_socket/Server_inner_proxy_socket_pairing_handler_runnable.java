package org.sun.pink.nat_penetration.server.runnable.inner_socket;

import org.sun.pink.nat_penetration.common.*;
import org.sun.pink.nat_penetration.server.Server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server_inner_proxy_socket_pairing_handler_runnable implements Runnable {

    public Server server;
    public SocketChannel conn;

    public Server_inner_proxy_socket_pairing_handler_runnable(Server server, SocketChannel conn) {

        this.server = server;
        this.conn = conn;
    }

    @Override
    public void run() {

        //开始配对
        //等待对方发送要配对的socket_id
        try {
            Simple_data_package packet = null;
            Simple_data_package_parser parser = new Simple_data_package_parser(this.server.maximum_data_packet_size);
            long end_time = System.currentTimeMillis() + this.server.socket_pair_pairing_wait_timeout_ms;
            //开始配对
            this.conn.configureBlocking(false);
            do {
                if (System.currentTimeMillis() > end_time) {
                    //连接失败，超时
                    System.out.println("等待inner_socket发送name超时");
                    Socket_util.Close_socket(this.conn);
                    return;
                }
                //解析
                packet = parser.parse(this.conn);
            } while (packet == null);
            //配对
            Pair_socket pair_socket = this.server.unpaired_socket_pair_map.get(packet.str_for_whatever_use);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            if (pair_socket == null) {
                //配对失败，没有此存在SOCKET_PAIR
                //发送回馈包
                buffer.clear();
                buffer.putInt(Global_error_code.CANT_FIND_UNPAIRED_SOCKET_PAIR_WITH_SPECIFIED_NAME);
                buffer.flip();
                //这里忽略返回值，因为回馈包发送成功不成功套接字都会被关闭
                Socket_util.Send_to_socket(this.conn, buffer, this.server.socket_pair_pairing_wait_timeout_ms);
                Socket_util.Close_socket(this.conn);
                //结束
                return;
            }
            //配对成功返回成功指令
            buffer.clear();
            buffer.putInt(Global_error_code.SUCCESS);
            buffer.flip();
            if (Socket_util.Send_to_socket(this.conn, buffer, this.server.socket_pair_pairing_wait_timeout_ms) == false) {
                //发送失败，返回
                Socket_util.Close_socket(this.conn);
                return;
            }
            //成功配对
            System.out.println("Inner socket配对成功");
            pair_socket.unsafe_inner_socket = this.conn;
        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Error, "inner_socket配对失败", Logger.Stringify(e));
        }
    }
}
