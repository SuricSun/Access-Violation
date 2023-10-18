package org.sun.pink.nat_penetration.server;

import org.sun.pink.nat_penetration.common.*;
import org.sun.pink.nat_penetration.server.runnable.control_socket.Server_control_socket_listener_runnable;
import org.sun.pink.nat_penetration.server.runnable.inner_socket.Server_inner_proxy_port_listener_runnable;
import org.sun.pink.nat_penetration.server.runnable.outer_socket.Server_outer_proxy_port_listener_runnable;
import org.sun.pink.nat_penetration.server.runnable.control_socket.Server_control_socket_dispatcher_runnable;
import org.sun.pink.nat_penetration.server.runnable.pair_socket.Server_pair_socket_dispatcher_runnable;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server {

    /**
     * 配置变量
     */
    public int client_control_socket_listen_port = 12364;

    //监听端口设置
    public int inner_proxy_listening_port = 12365;
    public List<Integer> outer_proxy_listening_ports = null;

    //router配置, socket addr -> p server name | socket addr -> socket_addr
    public List<Pair<Pattern, String>> router = null;

    //thread pool配置
    public int core_pool_size = 64;
    public int maximum_pool_size = 128;
    public int task_queue_size = 64;
    public int keep_alive_time_sec = 60;

    //包大小,不包括size的4字节
    public int maximum_data_packet_size = 1024 * 64; //64KB

    //send queue size
    public int send_queue_size = 128;
    public long send_queue_wait_timeout_ms = 1000;

    public int socket_pair_byte_buffer_size_byte = 1024 * 64;//64 KB

    //various time config
    public long control_socket_register_wait_timeout_ms = 5000;
    public long socket_pair_pairing_wait_timeout_ms = 5000;
    public long add_back_to_selector_wait_timeout_ms = 5000;
    public long selector_updated_time_interval = 5000;

    /**
     * 计数
     */
    public Sync_count sync_count = null;

    /**
     * nSelector机制
     */
    public Selector[] socket_pair_selectors = null;
    public Thread[] socket_pair_dispatcher_threads = null;
    public volatile Selector unsafe_socket_pair_selector_that_has_minimum_channels = null;
    /**
     * socket pair线程池
     */
    public ThreadPoolExecutor socket_pair_thread_pool = null;
    public ConcurrentHashMap<String, Pair_socket> unpaired_socket_pair_map = null;

    /**
     * nSelector机制
     */
    public Selector[] router_client_socket_selectors = null;
    public Thread[] router_client_socket_dispatcher_threads = null;
    public volatile Selector unsafe_router_client_selector_that_has_minimum_channels = null;
    /**
     * router_client_socket线程池
     */
    public ThreadPoolExecutor control_socket_thread_pool = null;
    /**
     * router_client_socket send queue map
     */
    public ConcurrentHashMap<String, Thread_safe_Fixed_capacity_Queue<ByteBuffer>> control_socket_send_queue_map = null;

    public void launch() {

        // * 启动服务器
        try {
            // * 初始化sync_count
            this.sync_count = new Sync_count();
            // * 初始化router
            this.router = new ArrayList<>();
            this.router.add(new Pair<>(Pattern.compile(".+"), "CLIENT_PINK"));
            // * 初始化send queue map
            this.unpaired_socket_pair_map = new ConcurrentHashMap<>();
            this.control_socket_send_queue_map = new ConcurrentHashMap<>();
            // * 初始化线程池 x 2
            this.socket_pair_thread_pool = new ThreadPoolExecutor(
                    this.core_pool_size,
                    this.maximum_pool_size,
                    this.keep_alive_time_sec,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(this.task_queue_size),
                    new ThreadPoolExecutor.AbortPolicy()
            );
            this.control_socket_thread_pool = new ThreadPoolExecutor(
                    this.core_pool_size,
                    this.maximum_pool_size,
                    this.keep_alive_time_sec,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(this.task_queue_size),
                    new ThreadPoolExecutor.AbortPolicy()
            );
            // * 初始化Selector x 4
            this.socket_pair_selectors = new Selector[2];
            this.socket_pair_selectors[0] = Selector.open();
            this.socket_pair_selectors[1] = Selector.open();
            this.unsafe_socket_pair_selector_that_has_minimum_channels = this.socket_pair_selectors[0];

            this.router_client_socket_selectors = new Selector[2];
            this.router_client_socket_selectors[0] = Selector.open();
            this.router_client_socket_selectors[1] = Selector.open();
            this.unsafe_router_client_selector_that_has_minimum_channels = this.router_client_socket_selectors[0];
            // * 初始化Dispatcher线程 x 2
            this.socket_pair_dispatcher_threads = new Thread[this.socket_pair_selectors.length];
            for (int i = 0; i < this.socket_pair_dispatcher_threads.length; i++) {
                this.socket_pair_dispatcher_threads[i] =
                        new Thread(new Server_pair_socket_dispatcher_runnable(this, i));
                this.socket_pair_dispatcher_threads[i].start();
            }

            this.router_client_socket_dispatcher_threads = new Thread[this.router_client_socket_selectors.length];
            for (int i = 0; i < this.router_client_socket_dispatcher_threads.length; i++) {
                this.router_client_socket_dispatcher_threads[i] =
                        new Thread(new Server_control_socket_dispatcher_runnable(this, i));
                this.router_client_socket_dispatcher_threads[i].start();
            }
            // * 初始化outer proxy socket listener线程
            this.outer_proxy_listening_ports = new ArrayList<>();
            this.outer_proxy_listening_ports.add(7070);
            for (Integer port : this.outer_proxy_listening_ports) {
                Thread t = new Thread(new Server_outer_proxy_port_listener_runnable(this, port));
                t.start();
            }
            // * 初始化inner proxy socket listener线程
            Thread inner_socket_listen_thread =
                    new Thread(new Server_inner_proxy_port_listener_runnable(this, this.inner_proxy_listening_port));
            inner_socket_listen_thread.start();
            // * 初始化router_client socket listener线程
            Thread router_client_socket_listen_thread =
                    new Thread(new Server_control_socket_listener_runnable(this, this.client_control_socket_listen_port));
            router_client_socket_listen_thread.start();

            // * 完成
            Logger.Log("file", Logger.Log_type.Info, "跑起来啦!!! 跑起来啦!!!");

            //TODO: BETTER WAY TO WAIT
            inner_socket_listen_thread.join();
            router_client_socket_listen_thread.join();

        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Info, "启动服务器失败", Logger.Stringify(e));
            System.exit(-1);
        }
    }

    /**
     * socket id
     *
     * @param socket_id
     * @return 找不到返回nullptr
     */
    public String route_to_router_client_friendly_name(String socket_id) {

        //遍历map找到第一个符合的pattern
        for (Pair<Pattern, String> pair : this.router) {
            if (pair.$0.matcher(socket_id).matches()) {
                return pair.$1;
            }
        }
        return null;
    }
}
