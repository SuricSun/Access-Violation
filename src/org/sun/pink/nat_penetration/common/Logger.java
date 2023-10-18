package org.sun.pink.nat_penetration.common;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author: SuricSun
 * @date: 2022/7/10
 */
public class Logger {

    private static class Wrapper {

        public List<OutputStream> outputStreams = null;
        public String line_break_chars = null;

        public Wrapper(List<OutputStream> outputStreams, String line_break_chars) {

            this.outputStreams = outputStreams;
            this.line_break_chars = line_break_chars;
        }
    }

    private static final ConcurrentMap<String, Wrapper> channel_name_to_streams = new ConcurrentHashMap<>();

    public enum Log_type {

        None, Info, Warning, Error
    }

    public static void Register_channel(String channel_name, String line_break_chars, OutputStream... output_streams) {

        Logger.Register_channel(channel_name, Arrays.asList(output_streams), line_break_chars);
    }

    public static void Register_channel(String channel_name, List<OutputStream> output_streams, String line_break_chars) {

        //TODO:如果已存在就抛出异常
        if (channel_name != null && output_streams != null) {
            Logger.channel_name_to_streams.put(channel_name, new Wrapper(output_streams, line_break_chars));
        }
    }

    public static String Get_formatted_log_string(Log_type log_type, String log_msg) {

        if (log_type == null) {
            log_type = Log_type.None;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");
        String time_str = dateFormat.format(new Date());
        String log_type_str = null;
        switch (log_type) {
            case Info:
                log_type_str = "[   Info]: ";
                break;
            case Warning:
                log_type_str = "[Warning]: ";
                break;
            case Error:
                log_type_str = "[  Error]: ";
                break;
            case None:
                log_type_str = "[   None]: ";
                break;
        }
        return time_str + log_type_str + log_msg;
    }

    public static void Log(String channel_name, Log_type log_type, String[] log_msgs, boolean insert_line_break_chars) {

        //枚举所有输出流
        Wrapper wrapper = Logger.channel_name_to_streams.get(channel_name);
        List<OutputStream> li = wrapper.outputStreams;
        if (li != null) {
            for (OutputStream output_stream : li) {
                synchronized (output_stream) {
                    try {
                        for (String log_msg : log_msgs) {
                            output_stream.write(Logger.Get_formatted_log_string(log_type, log_msg).getBytes(StandardCharsets.UTF_8));
                            if (insert_line_break_chars) {
                                output_stream.write(wrapper.line_break_chars.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(
                                Logger.Get_formatted_log_string(
                                        Log_type.Error,
                                        "频道<" + channel_name + ">无法写入输出流(" + e.getClass().getName() + ")"
                                )
                        );
                    }
                }
            }
        }
    }

    public static void Log(String channel_name, Log_type log_type, String... log_msgs) {

        Logger.Log(channel_name, log_type, log_msgs, true);
    }

    public static void Log(OutputStream log_to_stream, Log_type log_type, String log_msg) {

        try {
            log_to_stream.write(Logger.Get_formatted_log_string(log_type, log_msg).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String Stringify(Exception e) {

        return e.getClass().getCanonicalName()
                + ": "
                + e.getMessage()
                + "\n"
                + Logger.Stringify(e.getStackTrace(), 0);
    }

    /**
     * 如果偏移大于等于数组长度或者小于0，便宜会被设置为0
     *
     * @param stack_trace_elem_arr
     * @param stringify_offset_from_0
     * @return
     */
    public static String Stringify(StackTraceElement[] stack_trace_elem_arr, int stringify_offset_from_0) {

        if (stack_trace_elem_arr == null) {
            return null;
        }

        if (stringify_offset_from_0 >= stack_trace_elem_arr.length || stringify_offset_from_0 < 0) {
            stringify_offset_from_0 = 0;
        }

        StringBuilder sb = new StringBuilder();
        for (; stringify_offset_from_0 < stack_trace_elem_arr.length; stringify_offset_from_0++) {
            sb.append("at ").append(stack_trace_elem_arr[stringify_offset_from_0]).append("\n");
        }

        return sb.toString();
    }
}
