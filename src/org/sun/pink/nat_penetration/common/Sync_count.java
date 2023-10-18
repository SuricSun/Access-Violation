package org.sun.pink.nat_penetration.common;

/**
 * @author: SuricSun
 * @date: 2022/7/27
 */
public class Sync_count {

    private volatile long pair_socket_count = 0;
    private volatile long control_socket_count = 0;

    public void incr_pair_socket_count() {

        synchronized (this) {
            this.pair_socket_count++;
        }
    }

    public void incr_control_socket_count() {
        synchronized (this) {
            this.control_socket_count++;
        }
    }

    public void decr_pair_socket_count() {

        synchronized (this) {
            this.pair_socket_count--;
        }
    }

    public void decr_control_socket_count() {
        synchronized (this) {
            this.control_socket_count--;
        }
    }

    public long get_socket_pair_count() {

        return pair_socket_count;
    }

    public long get_control_socket_count() {

        return control_socket_count;
    }

    @Override
    public String toString() {

        synchronized (this) {
            return "Sync_count{" +
                    "pair_socket_count=" + pair_socket_count +
                    ", control_socket_count=" + control_socket_count +
                    '}';
        }
    }
}
