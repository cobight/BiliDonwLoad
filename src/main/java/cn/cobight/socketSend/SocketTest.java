package cn.cobight.socketSend;

import cn.cobight.Util.SocketGetTools;
import java.io.*;
//音频要有host
public class SocketTest {
    public static void main(String[] args) throws Exception {
        mix();
    }
    public static void downloadaudio() throws IOException {
        String url = "http://cn-jxjj-dx-v-11.bilivideo.com/upgcxcode/31/21/225402131/225402131_nb2-1-30280.m4s?expires=1599308659&platform=pc&ssig=3O8VmWKHDZtJEGZEdrOUQw&oi=29838136&trid=9357bfa6f3fd482f957fe696a6320bafu&nfc=1&nfb=maPYqpoel5MI3qOUX6YpRA==&cdnid=9622&mid=0&cip=117.21.179.14&orderid=0,3&agrr=1&logo=80000000";
        SocketGetTools socketTools = new SocketGetTools(url);
        socketTools.getHost();
        socketTools.setHeader("Connection", "keep-alive");
        socketTools.setHeader("Origin","https://www.bilibili.com");
        socketTools.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        socketTools.setHeader("Accept", "*/*");
        socketTools.setHeader("Referer","https://www.bilibili.com/video/BV1o5411873u");
        socketTools.setHeader("Accept-encoding", "gzip, deflate, br");
        socketTools.setHeader("Accept-language", "zh-CN,zh;q=0.9");
        socketTools.setHeader("Range","bytes=0-3726969");
        socketTools.sendGet();
        System.out.println("加载完毕");
        socketTools.writeToFile("download/audios.m4s");
    }
    public static void downloadvideo() throws IOException {
        String url = "https://cn-jxjj-dx-v-10.bilivideo.com/upgcxcode/31/21/225402131/225402131_nb2-1-30080.m4s?expires=1599313359&platform=pc&ssig=_UePdsCXzKpHCthfUHA1jg&oi=29838136&trid=a5fd444abec54a30baa9acb0b0e77dc2u&nfc=1&nfb=maPYqpoel5MI3qOUX6YpRA==&cdnid=9621&mid=96913634&cip=117.21.179.13&orderid=0,3&agrr=0&logo=80000000";
        SocketGetTools socketTools = new SocketGetTools(url);
        socketTools.getHost();
        socketTools.setHeader("Connection", "keep-alive");
        socketTools.setHeader("Origin","https://www.bilibili.com");
        socketTools.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        socketTools.setHeader("Accept", "*/*");
        socketTools.setHeader("Referer","https://www.bilibili.com/video/BV1o5411873u");
        socketTools.setHeader("Accept-encoding", "gzip, deflate, br");
        socketTools.setHeader("Accept-language", "zh-CN,zh;q=0.9");
        socketTools.setHeader("Range","bytes=0-60183019");
        socketTools.sendGet();
        System.out.println("加载完毕");
        socketTools.writeToFile("download/videos.m4s");
    }
    public static void mix(){
        Runtime runtime = Runtime.getRuntime();
        String cmdstr = "ffmpeg -i download/videos.m4s -i download/audios.m4s -codec copy download/out.flv";
        try {
            runtime.exec("download/ffmpeg.exe -i download/video.m4s -i download/audio.m4s -codec copy download/out.mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }

//ffmpeg -i videos.m4s -i audios.m4s -codec copy out.mp4
//F:/JAVA/secondstage/BiliDonwLoad/download/ffmpeg.exe -i F:/JAVA/secondstage/BiliDonwLoad/download/videos.m4s -i F:/JAVA/secondstage/BiliDonwLoad/download/audios.m4s -codec copy F:/JAVA/secondstage/BiliDonwLoad/download/out.mp4
    }


}


