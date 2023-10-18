package org.sun.pink.nat_penetration.server.runnable.outer_socket;

import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.common.Pair_socket;
import org.sun.pink.nat_penetration.common.Socket_util;
import org.sun.pink.nat_penetration.server.Server;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Server_outer_proxy_port_listener_runnable implements Runnable {

    public Server server;
    public int port;

    public Server_outer_proxy_port_listener_runnable(Server server, int port) {

        this.server = server;
        this.port = port;
    }

    @Override
    public void run() {

        ServerSocketChannel listener;
        //循环等待连接
        try {
            listener = ServerSocketChannel.open();
            listener.bind(new InetSocketAddress(this.port));
            while (true) {
                //等待连接
                SocketChannel new_conn = listener.accept();
                //包装此channel
                Pair_socket pair = new Pair_socket();
                pair.outer_socket = new_conn;
                pair.outer_socket_id = Socket_util.Get_addr_string(new_conn);
                //TODO:delete me
                Logger.Log("file", Logger.Log_type.Info, pair.outer_socket_id + "已连接");
                //路由此socket
                String dst_client_friendly_name = this.server.route_to_router_client_friendly_name(pair.outer_socket_id);
                if (dst_client_friendly_name == null) {
                    //无法路由，断开连接
                    //TODO:delete me
                    Logger.Log("file", Logger.Log_type.Warning, pair.outer_socket_id + "无法找到合适的路由, 已断开");
                    Socket_util.Close_socket(new_conn);
                    //继续下个请求
                    continue;
                }
                pair.router_client_socket_friendly_name_to_route_to = dst_client_friendly_name;
                //检查路由到的bridge是否存在
                //存在才继续
                if (this.server.control_socket_send_queue_map.containsKey(dst_client_friendly_name)) {
                    //调用线程池执行配对操作
                    this.server.unpaired_socket_pair_map.put(pair.outer_socket_id, pair);
                    try {
                        this.server.socket_pair_thread_pool.execute(
                                new Server_outer_proxy_socket_pairing_handler_runnable(this.server, pair)
                        );
                    } catch (RejectedExecutionException e) {
                        Logger.Log("file", Logger.Log_type.Warning, "线程池满，无法处理Outer_Socket连接请求");
                        //断开连接
                        Socket_util.Close_socket(pair.outer_socket);
                        //继续下个请求
                        continue;
                    }
                    //TODO:delete me
                    Logger.Log("file", Logger.Log_type.Warning, pair.outer_socket_id + "代理成功");
                } else {
                    //TODO:delete me
                    Logger.Log("file", Logger.Log_type.Warning, pair.outer_socket_id + "路由的桥套接字不存在, 已断开");
                    //继续下个请求
                    continue;
                }
            }
        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Error, "服务器异常终止", Logger.Stringify(e));
            System.exit(-1);
        }
    }
}
