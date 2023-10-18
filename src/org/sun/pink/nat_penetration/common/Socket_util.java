package org.sun.pink.nat_penetration.common;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 * @author: SuricSun
 * @date: 2022/7/31
 */
public class Socket_util {

    public static void Close_socket(AbstractSelectableChannel channel) {

        if (channel == null) {
            return;
        }

        try {
            channel.close();
        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Warning, "套接字关闭异常", Logger.Stringify(e));
        }
    }

    /**
     * 不会close socket，需要手动关闭
     *
     * @param buffer
     * @param send_time_ms
     * @return true: 发送成功 false: 发送超时或对方断开连接
     */
    public static boolean Send_to_socket(SocketChannel socket, ByteBuffer buffer, long send_time_ms) {

        if (socket == null) {
            return false;
        }

        long end_time = System.currentTimeMillis() + send_time_ms;

        while (buffer.hasRemaining()) {
            if (System.currentTimeMillis() > end_time) {
                //连接失败，超时
                System.out.println("Send_to_socket超时");
                return false;
            }
            //发送回馈
            try {
                socket.write(buffer);
            } catch (Exception e) {
                System.out.println("Send_to_socket时断开连接");
                return false;
            }
        }

        return true;
    }

    /**
     * @param socket
     * @return 发生异常或者socket未连接返回null
     */
    public static String Get_addr_string(SocketChannel socket) {

        try {
            InetSocketAddress local = (InetSocketAddress) socket.getLocalAddress();
            InetSocketAddress remote = (InetSocketAddress) socket.getRemoteAddress();
            if (local != null && remote != null) {
                String composed_string = local.getAddress().getHostAddress() + ":" + local.getPort();
                composed_string += "<->";
                composed_string += remote.getAddress().getHostAddress() + ":" + remote.getPort();
                return composed_string;
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.Log("file", Logger.Log_type.Error, "Get_addr_string异常", e.getMessage());
            return null;
        }
    }
}
