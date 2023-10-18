package org.sun.pink.nat_penetration.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class Simple_data_package {

    /**
     * size大小不包括size本身的4字节大小
     */
    public int size = 0;
    public String str_for_whatever_use = "";

    public Simple_data_package(){

    }

    @Override
    public String toString() {
        return "Simple_data_packet{" +
                "size=" + size +
                ", socket_id='" + str_for_whatever_use + '\'' +
                '}';
    }

    public static ByteBuffer Generate_byte_buffer_by(String socket_id){

        byte[] bytes = socket_id.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        bb.clear();
        bb.putInt(bytes.length);
        bb.put(bytes);
        bb.flip();
        return bb;
    }
}
