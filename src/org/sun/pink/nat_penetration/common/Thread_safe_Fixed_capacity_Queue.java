package org.sun.pink.nat_penetration.common;

/**
 * @author: SuricSun
 * @date: 2022/7/10
 */
public class Thread_safe_Fixed_capacity_Queue<T> {

    private T[] inner_queue_array = null;
    private int start_idx = 0;
    private int length = 0;

    private final int DEFAULT_CAPACITY = 8;

    public final long INFINITE_TIME = Long.MAX_VALUE;

    private Object attachment = null;

    public Thread_safe_Fixed_capacity_Queue() {

        this.inner_queue_array = (T[]) new Object[this.DEFAULT_CAPACITY];
    }

    public Thread_safe_Fixed_capacity_Queue(int capacity) {

        this.inner_queue_array = (T[]) new Object[capacity];
    }

    public Thread_safe_Fixed_capacity_Queue(int capacity, Object attachment) {

        this.inner_queue_array = (T[]) new Object[capacity];
        this.attachment = attachment;
    }

    public boolean put(T obj) {

        return this.put(obj, this.INFINITE_TIME);
    }

    public T get() {

        return this.get(this.INFINITE_TIME);
    }

    /**
     * put a obj to the tail of the queue, if there is no space available,
     * it'll be stuck util some space available or the wait time is up
     *
     * @param obj
     * @param wait_time_ms
     * @return true if put successfully, false if there is no space available during wait_time_ms interval
     */
    public boolean put(T obj, long wait_time_ms) {

        //确保当wait_time_ms为0的时候，下面的(System.currentTimeMillis() - time_marker_ms)一定大于0
        long time_marker_ms = System.currentTimeMillis() - 1;
        while (true) {
            synchronized (this) {
                if (this.length >= this.inner_queue_array.length) {
                    //队列满了，一直轮询直到等待时间结束
                    if ((System.currentTimeMillis() - time_marker_ms) > wait_time_ms) {
                        //等待时间到了，退出
                        return false;
                    }
                } else {
                    //队列有空, 插入
                    int insert_position = (this.start_idx + this.length) % this.inner_queue_array.length;
                    this.inner_queue_array[insert_position] = obj;
                    this.length++;
                    return true;
                }
            }
        }
    }

    /**
     * get a object from the head of the queue, if there is nothing in the queue,
     * it'll be stuck util some objects available or the wait time is up
     *
     * @param wait_time_ms
     * @return obj or null if wait time is up and there is nothing here in the queue
     */
    public T get(long wait_time_ms) {

        //确保当wait_time_ms为0的时候，下面的(System.currentTimeMillis() - time_marker_ms)一定大于0
        long time_marker_ms = System.currentTimeMillis() - 1;
        while (true) {
            synchronized (this) {
                if (this.length <= 0) {
                    //队列空了，一直轮询直到等待时间结束
                    if ((System.currentTimeMillis() - time_marker_ms) > wait_time_ms) {
                        //等待时间到了，退出
                        return null;
                    }
                } else {
                    //队列有空, 获取
                    int original_start_idx = this.start_idx;
                    this.start_idx = (this.start_idx + 1) % this.inner_queue_array.length;
                    this.length--;
                    return this.inner_queue_array[original_start_idx];
                }
            }
        }
    }

    public Object get_attachment() {
        return attachment;
    }

    public void set_attachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {

        String str = "";
        for (int i = 0; i < this.length; i++) {
            int right_idx = (start_idx + i) % this.inner_queue_array.length;
            str += this.inner_queue_array[right_idx].toString() + "\t";
        }
        return str;
    }
}
