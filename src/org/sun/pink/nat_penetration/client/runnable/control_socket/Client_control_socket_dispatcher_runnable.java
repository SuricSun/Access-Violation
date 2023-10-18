package org.sun.pink.nat_penetration.client.runnable.control_socket;

import org.sun.pink.nat_penetration.client.Client;
import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.common.Control_socket;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Client_control_socket_dispatcher_runnable implements Runnable {

    public Client client;
    public Selector selector;

    public Client_control_socket_dispatcher_runnable(Client client, int selector_idx) {

        this.client = client;
        this.selector = client.control_socket_selectors[selector_idx];
    }

    @Override
    public void run() {

        //循环分发selector
        long end_time = -1;
        while (true) {
            try {
                //更新
                if (System.currentTimeMillis() > end_time) {
                    synchronized (this.client.unsafe_server_socket_selector_that_has_minimum_channels) {
                        if (this.selector.keys().size() < this.client.unsafe_server_socket_selector_that_has_minimum_channels.keys().size()) {
                            this.client.unsafe_server_socket_selector_that_has_minimum_channels = this.selector;
                        }
                    }
                    end_time = System.currentTimeMillis() + this.client.selector_updated_time_interval;
                }
                if (this.selector.select(100) > 0) {
                    Set<SelectionKey> selected_keys = this.selector.selectedKeys();
                    Iterator<SelectionKey> it = selected_keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        //设置ready ops
                        Control_socket control_socket = (Control_socket) key.attachment();
                        int ready_ops = key.readyOps();
                        //分发到相应的线程池
                        try {
                            this.client.control_socket_thread_pool.execute(
                                    new Client_control_socket_rw_handler_runnable(
                                            this.client,
                                            control_socket,
                                            key,
                                            ready_ops
                                    )
                            );
                        } catch (RejectedExecutionException e) {
                            Logger.Log("file", Logger.Log_type.Info, "线程池满，无法Dispatch此Control_Socket");
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
