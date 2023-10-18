package org.sun.pink.nat_penetration.client.runnable.pair_socket;

import org.sun.pink.nat_penetration.client.Client;
import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.common.Pair_socket;

import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Client_pair_socket_rw_handler_runnable implements Runnable {

    public Client client;
    public SocketChannel ready_socket;
    public SelectionKey ready_key;
    public int ready_ops;
    public Pair_socket pair_socket;

    public Client_pair_socket_rw_handler_runnable(Client client, SocketChannel ready_socket, SelectionKey ready_key, int ready_ops, Pair_socket pair_socket) {

        this.client = client;
        this.ready_socket = ready_socket;
        this.ready_key = ready_key;
        this.ready_ops = ready_ops;
        this.pair_socket = pair_socket;
    }

    @Override
    public void run() {

        if (this.read() == false) {
            this.pair_socket.self_cleanup();
            //socket_pair断开
            return;
        }
        //设置selector对此key的监听事件
        this.ready_key.interestOps(SelectionKey.OP_READ);
    }

    public boolean read() {

        //读取并放入
        SocketChannel from, to;
        ByteBuffer buffer;
        if (this.ready_socket == this.pair_socket.outer_socket) {
            from = this.pair_socket.outer_socket;
            to = this.pair_socket.unsafe_inner_socket;
            buffer = this.pair_socket.outer2inner_buffer;
        } else {
            from = this.pair_socket.unsafe_inner_socket;
            to = this.pair_socket.outer_socket;
            buffer = this.pair_socket.inner2outer_buffer;
        }
        //读取并写入
        try {
            buffer.clear();
            //TODO: LOOP START
            int read = from.read(buffer);
            if (read == -1) {
                //断开连接
                throw new Exception("Pair_socket read returns -1");
            }
            if (buffer.position() != 0) {
                //写入
                buffer.flip();
                to.write(buffer);
                //把剩余的移到最前面
                buffer.compact();
            }
            //TODO: LOOP END
        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Error, "Socket_pair断开连接: " + this.pair_socket.outer_socket_id, Logger.Stringify(e));
            this.pair_socket.self_cleanup();
            return false;
        }

        return true;
    }
}
