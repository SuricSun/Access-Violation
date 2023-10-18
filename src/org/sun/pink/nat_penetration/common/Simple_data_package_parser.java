package org.sun.pink.nat_penetration.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Simple_data_package_parser {

    /**
     * 不包括size本身的4字节
     */
    private int maximum_data_packet_size = 128;

    private boolean parsing_in_progress = false;
    private boolean ignore_current_data_packet = false;

    private ByteBuffer buffer = null;

    public Simple_data_package_parser() {

        this.buffer = ByteBuffer.allocate(this.maximum_data_packet_size + Integer.BYTES);
    }

    /**
     * @param maximum_data_packet_size 不包括size本身的4字节
     */
    public Simple_data_package_parser(int maximum_data_packet_size) {

        this.maximum_data_packet_size = maximum_data_packet_size;
        this.buffer = ByteBuffer.allocate(this.maximum_data_packet_size + Integer.BYTES);
        this.buffer.clear();
        this.buffer.limit(Integer.BYTES);
    }

    public Simple_data_package parse(SocketChannel conn) throws Exception {

        int bytes_to_read = -1;
        if (this.parsing_in_progress == false) {
            //尝试读取新的包大小
            if (conn.read(this.buffer) == -1) {
                //连接断开
                throw new IOException("套接字已被关闭");
            }
            if (this.buffer.hasRemaining() == false) {
                //读满了，开始获取size
                this.buffer.position(0);
                bytes_to_read = this.buffer.getInt();
                if (bytes_to_read < 1 || bytes_to_read > this.maximum_data_packet_size) {
                    //TODO: delete
                    Logger.Log(
                            "file",
                            Logger.Log_type.Warning,
                            "Simple_data_packet_parser parse a illegal size packet: " +
                                    bytes_to_read
                    );
                    //设置忽略
                    this.ignore_current_data_packet = true;
                } else {
                    this.buffer.limit(Integer.BYTES + bytes_to_read);
                    this.buffer.position(Integer.BYTES);
                }
                this.parsing_in_progress = true;
                return null;
            }
        } else {
            //读取packet body
            if (this.buffer.hasRemaining()) {
                conn.read(this.buffer);
            }
            //是否可以真正解析
            if (this.buffer.hasRemaining() == false) {
                //设置状态
                this.parsing_in_progress = false;
                Simple_data_package ret = null;
                if (this.ignore_current_data_packet == false) {
                    ret = this.parse_inner();
                } else {
                    this.ignore_current_data_packet = false;
                }
                this.buffer.clear();
                this.buffer.limit(Integer.BYTES);
                return ret;
            }
        }
        return null;
    }

    private Simple_data_package parse_inner() {

        //读取大小和String
        Simple_data_package simple_data_package = new Simple_data_package();
        this.buffer.position(0);
        simple_data_package.size = this.buffer.getInt();
        simple_data_package.str_for_whatever_use = new String(
                this.buffer.array(),
                this.buffer.position(),
                simple_data_package.size,
                StandardCharsets.UTF_8
        );
        return simple_data_package;
    }
}
