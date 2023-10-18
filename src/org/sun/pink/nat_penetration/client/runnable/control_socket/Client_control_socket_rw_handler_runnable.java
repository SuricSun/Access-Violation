package org.sun.pink.nat_penetration.client.runnable.control_socket;

import org.sun.pink.nat_penetration.client.Client;
import org.sun.pink.nat_penetration.client.runnable.pair_socket.Client_pair_socket_pairing_handler_runnable;
import org.sun.pink.nat_penetration.common.*;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Client_control_socket_rw_handler_runnable implements Runnable {

    public Client client;
    public Control_socket control_socket;
    SelectionKey ready_key;
    public int ready_ops;

    public Client_control_socket_rw_handler_runnable(Client client, Control_socket control_socket, SelectionKey ready_key, int ready_ops) {

        this.client = client;
        this.control_socket = control_socket;
        this.ready_key = ready_key;
        this.ready_ops = ready_ops;
    }

    @Override
    public void run() {

        if (this.read() == false) {
            this.control_socket.self_cleanup();
            return;
        }
        //设置selector对此key的监听事件
        this.ready_key.interestOps(SelectionKey.OP_READ);
    }

    public boolean read() {

        if ((this.ready_ops & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
            return true;
        }

        //读取请求并创建新的Socket_pair
        try {
            Simple_data_package pack = this.control_socket.parser.parse(this.control_socket.conn);
            if (pack == null) {
                return true;
            }
            //收到穿透请求
            System.out.println("收到穿透请求");
            //创建新的socket_pair
            //路由
            Pair_socket pair_socket = new Pair_socket();
            Simple_socket_addr outer_socket_addr = this.client.route_to_outer_socket_addr(pack.str_for_whatever_use);
            if (outer_socket_addr == null) {
                System.out.println("无法路由");
                return true;
            }
            pair_socket.inner_socket_id = pack.str_for_whatever_use;
            pair_socket.outer_socket_addr = outer_socket_addr;
            pair_socket.server_to_connect_to = this.control_socket.server_socket_addr;
            //开始配对
            try {
                this.client.pair_socket_thread_pool.execute(
                        new Client_pair_socket_pairing_handler_runnable(
                                this.client,
                                pair_socket
                        )
                );
            } catch (RejectedExecutionException e) {
                Logger.Log("file", Logger.Log_type.Info, "线程池满，无法为服务器创建配对套接字");
                return true;
            }
        } catch (Exception e) {
            System.out.println("server_socket断开连接: " + Logger.Stringify(e));
            return false;
        }

        return true;
    }
}
