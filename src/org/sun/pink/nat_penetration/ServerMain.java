package org.sun.pink.nat_penetration;

import org.sun.pink.nat_penetration.common.Logger;
import org.sun.pink.nat_penetration.server.Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

/**
 * @author: SuricSun
 * @date: 2022/7/25
 */
public class ServerMain {

    public static void main(String[] args) throws FileNotFoundException {

        //创建日志文件
        System.out.println(
                "     _____    __     __    __   __  __     ____\n" +
                "    /    /    /     / |    /    /  .'    /    /\n" +
                "   /____/    /     /  |   /    /_.'       ___/ \n" +
                "  /         /     /   |  /    /  \\      /     \n" +
                "_/_       _/_   _/_   |_/   _/_  _\\_   /____/ \n");

        String file_name = System.getProperty("user.dir")
                + File.separator
                + "PINK2_Server_Log"
                //+ new SimpleDateFormat("_yyyy年MM月dd日HH时mm分ss秒").format(System.currentTimeMillis())
                + ".txt";
        FileOutputStream fo = new FileOutputStream(file_name);
        System.out.println("PINK2> Log file created at " + file_name);
        //注册Logger通道，把输出定向到日志文件
        Logger.Register_channel("file", "\r\n", fo, System.out);
        Server server = new Server();
        server.launch();
    }
}
