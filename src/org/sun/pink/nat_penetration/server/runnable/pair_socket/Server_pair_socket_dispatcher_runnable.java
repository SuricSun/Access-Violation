package org.sun.pink.nat_penetration.server.runnable.pair_socket;

import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.common.Pair_socket;
import org.sun.pink.nat_penetration.server.Server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server_pair_socket_dispatcher_runnable implements Runnable {

    public Server server;
    public Selector selector;

    public Server_pair_socket_dispatcher_runnable(Server server, int selector_idx) {

        this.server = server;
        this.selector = server.socket_pair_selectors[selector_idx];
    }

    @Override
    public void run() {

        //循环分发selector
        long end_time = -1;
        while (true) {
            try {
                //更新
                if (System.currentTimeMillis() > end_time) {
                    synchronized (this.server.unsafe_socket_pair_selector_that_has_minimum_channels) {
                        if (this.selector.keys().size() < this.server.unsafe_socket_pair_selector_that_has_minimum_channels.keys().size()) {
                            this.server.unsafe_socket_pair_selector_that_has_minimum_channels = this.selector;
                        }
                    }
                    end_time = System.currentTimeMillis() + this.server.selector_updated_time_interval;
                }
                if (this.selector.select(100) > 0) {
                    Set<SelectionKey> selected_keys = this.selector.selectedKeys();
                    Iterator<SelectionKey> it = selected_keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        //设置ready ops
                        Pair_socket pair_socket = (Pair_socket) key.attachment();
                        int ready_ops = key.readyOps();
                        //分发到相应的线程池
                        try {
                            this.server.socket_pair_thread_pool.execute(
                                    new Server_pair_socket_rw_handler_runnable(
                                            this.server,
                                            (SocketChannel) key.channel(),
                                            key,
                                            ready_ops,
                                            pair_socket
                                    )
                            );
                        } catch (RejectedExecutionException e) {
                            Logger.Log("file", Logger.Log_type.Info, "线程池满，无法Dispatch此Socket_Pair");
                            //将这个key保留在selectedKeys集合以供下次继续读取
                            //继续下个请求
                            continue;
                        }
                        //取消此key的监听事件
                        key.interestOps(0);
                        //移除这个key
                        it.remove();
                    }
                }
            } catch (Exception e) {
                Logger.Log("file", Logger.Log_type.Error, "服务器异常终止", Logger.Stringify(e));
                System.exit(-1);
            }
        }
    }
}
