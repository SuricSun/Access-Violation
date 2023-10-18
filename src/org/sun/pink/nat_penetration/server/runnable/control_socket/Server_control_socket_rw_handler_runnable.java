package org.sun.pink.nat_penetration.server.runnable.control_socket;

import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.common.Control_socket;
import org.sun.pink.nat_penetration.common.Thread_safe_Fixed_capacity_Queue;
import org.sun.pink.nat_penetration.server.Server;

import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Random;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server_control_socket_rw_handler_runnable implements Runnable {

    public Server server;
    public Control_socket control_socket;
    public int ready_ops;
    public SelectionKey ready_key;

    public Server_control_socket_rw_handler_runnable(Server server, Control_socket control_socket, SelectionKey ready_key, int ready_ops) {

        this.server = server;
        this.control_socket = control_socket;
        this.ready_key = ready_key;
        this.ready_ops = ready_ops;
    }

    @Override
    public void run() {

        if (this.read() == false) {
            System.out.println(this.control_socket.registered_name + "已经下线");
            this.control_socket.self_cleanup();
            //抹除queue
            this.server.control_socket_send_queue_map.remove(this.control_socket.registered_name);
            return;
        }

        if (this.write() == false) {
            System.out.println(this.control_socket.registered_name + "已经下线");
            this.control_socket.self_cleanup();
            //抹除queue
            this.server.control_socket_send_queue_map.remove(this.control_socket.registered_name);
            return;
        }
        //设置selector对此key的监听事件
        this.ready_key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public boolean read() {

        if ((this.ready_ops & SelectionKey.OP_READ) != SelectionKey.OP_READ) {
            return true;
        }

        //检查是否断开连接
        try {
            if (this.control_socket.conn.read(this.control_socket.$1byte_buffer) == -1) {
                //断开连接
                return false;
            }
        } catch (Exception e) {
            System.out.println("read异常: " + Logger.Stringify(e));
            return false;
        }

        return true;
    }

    public boolean write() {

        if ((this.ready_ops & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE) {
            return true;
        }

        //如果有未完成的先发送未完成的
        ByteBuffer buffer_to_send = null;
        if (this.control_socket.unfinished_write_buffer != null) {
            //继续发送
            buffer_to_send = this.control_socket.unfinished_write_buffer;
        } else {
            //从queue里面拿包裹并发送
            Thread_safe_Fixed_capacity_Queue<ByteBuffer> q =
                    this.server.control_socket_send_queue_map.get(this.control_socket.registered_name);
            if (q == null) {
                Logger.Log(
                        "file",
                        Logger.Log_type.Error,
                        "服务器异常终止",
                        "map中不存在router_client的send_queue",
                        Logger.Stringify(Thread.currentThread().getStackTrace(), 1)
                );
                System.exit(-1);
            }
            //等待一秒
            ByteBuffer buffer_from_q = q.get(this.server.send_queue_wait_timeout_ms);
            if (buffer_from_q == null) {
                //没有就直接返回
                return true;
            } else {
                buffer_to_send = buffer_from_q;
            }
        }
        //写数据
        try {
            //这里写的是raw_buffer，而edge端写的是payload
            this.control_socket.conn.write(buffer_to_send);
            if (buffer_to_send.hasRemaining()) {
                //没发送完下次继续
                this.control_socket.unfinished_write_buffer = buffer_to_send;
            } else {
                //发送完下次从queue里面拿
                System.out.println("成功发送一个打洞请求");
                this.control_socket.unfinished_write_buffer = null;
            }
        } catch (Exception e) {
            //socket断连
            //抹除socket
            this.control_socket.self_cleanup();
            return false;
        }

        return true;
    }
}
