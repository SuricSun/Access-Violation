package org.sun.pink.nat_penetration.common;

/**
 * ip default to "", port default to -1
 * @author: SuricSun
 * @date: 2022/7/26
 */
public class Simple_socket_addr {

    public String ip = "";
    public int port0 = -1;
    public int port1 = -1;

    public Simple_socket_addr(){

    }

    public Simple_socket_addr(String ip, int port0) {

        this.ip = ip;
        this.port0 = port0;
    }

    public Simple_socket_addr(String ip, int port0, int port1) {

        this.ip = ip;
        this.port0 = port0;
        this.port1 = port1;
    }
}
