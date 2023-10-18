package org.sun.pink.nat_penetration.client;

import org.sun.pink.nat_penetration.client.runnable.control_socket.Client_control_socket_dispatcher_runnable;
import org.sun.pink.nat_penetration.client.runnable.control_socket.Client_control_socket_register_handler_runnable;
import org.sun.pink.nat_penetration.client.runnable.pair_socket.Client_pair_socket_dispatcher_runnable;
import org.sun.pink.nat_penetration.common.*;

import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Client {

    /**
     * 配置变量
     */
    public List<Pair<String, Simple_socket_addr>> server_to_connect_to = null;

    //router配置, socket addr -> p server name | socket addr -> socket_addr
    public List<Pair<Pattern, Simple_socket_addr>> router = null;

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

    public int socket_pair_byte_buffer_size_byte = 1024 * 8;//8 KB

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
    public Thread[] pair_socket_dispatcher_threads = null;
    public volatile Selector unsafe_socket_pair_selector_that_has_minimum_channels = null;
    /**
     * socket pair线程池
     */
    public ThreadPoolExecutor pair_socket_thread_pool = null;

    /**
     * nSelector机制
     */
    public Selector[] control_socket_selectors = null;
    public Thread[] control_socket_dispatcher_threads = null;
    public volatile Selector unsafe_server_socket_selector_that_has_minimum_channels = null;
    /**
     * server_socket线程池
     */
    public ThreadPoolExecutor control_socket_thread_pool = null;

    public void launch() {

        // * 启动服务器
        try {
            // * 初始化sync_count
            this.sync_count = new Sync_count();
            // * 初始化要连接的服务器
            this.server_to_connect_to = new ArrayList<>();
            this.server_to_connect_to.add(
                    new Pair<>(
                            "CLIENT_PINK",
                            new Simple_socket_addr("127.0.0.1", 12364, 12365)
                    )
            );
            // * 初始化router
            this.router = new ArrayList<>();
            this.router.add(new Pair<>(Pattern.compile(".+"), new Simple_socket_addr("127.0.0.1", 7521)));
            // * 初始化线程池 x 2
            this.pair_socket_thread_pool = new ThreadPoolExecutor(
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

            this.control_socket_selectors = new Selector[2];
            this.control_socket_selectors[0] = Selector.open();
            this.control_socket_selectors[1] = Selector.open();
            this.unsafe_server_socket_selector_that_has_minimum_channels = this.control_socket_selectors[0];
            // * 初始化Dispatcher线程 x 2
            this.pair_socket_dispatcher_threads = new Thread[this.socket_pair_selectors.length];
            for (int i = 0; i < this.pair_socket_dispatcher_threads.length; i++) {
                this.pair_socket_dispatcher_threads[i] =
                        new Thread(new Client_pair_socket_dispatcher_runnable(this, i));
                this.pair_socket_dispatcher_threads[i].start();
            }

            this.control_socket_dispatcher_threads = new Thread[this.control_socket_selectors.length];
            for (int i = 0; i < this.control_socket_dispatcher_threads.length; i++) {
                this.control_socket_dispatcher_threads[i] =
                        new Thread(new Client_control_socket_dispatcher_runnable(this, i));
                this.control_socket_dispatcher_threads[i].start();
            }
            // * 初始化连接到server线程
            for (int i = 0; i < this.server_to_connect_to.size(); i++) {
                Thread t = new Thread(
                        new Client_control_socket_register_handler_runnable(
                                this,
                                this.server_to_connect_to.get(i).$1,
                                this.server_to_connect_to.get(i).$0
                        )
                );
                t.start();
            }
            // * 完成
            Logger.Log("file", Logger.Log_type.Info, "跑起来啦!!! 跑起来啦!!!");

            //TODO: BETTER WAY TO WAIT
            this.control_socket_dispatcher_threads[0].join();

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
    public Simple_socket_addr route_to_outer_socket_addr(String socket_id) {

        //遍历map找到第一个符合的pattern
        for (Pair<Pattern, Simple_socket_addr> pair : this.router) {
            if (pair.$0.matcher(socket_id).matches()) {
                return pair.$1;
            }
        }
        return null;
    }
}
