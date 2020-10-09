package cn.cobight.Version1;


import cn.cobight.Util.RequestTool;
import cn.cobight.Util.SocketSpiderTool;
import com.jayway.jsonpath.JsonPath;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * fileName:BiliTool
 * description: 利用socket模拟浏览器发送请求，下载bili视频并合成
 * 第一个版本，音频与视频的m4s文件未拆包，所以视频会下的比较慢，所以version为1.0.0版本
 * author:cobight
 * createTime:2020/9/4 14:18
 * version:1.0.0
 */
public class BiliTool {
    static {
        System.out.println("Version : 1");
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        downbili("BV1o5411873u");


    }

    public static void downbili(String bvPath) {
        String BV = "https://www.bilibili.com/video/" + bvPath;
        //第一个请求偷下懒，没用socket
        String HTML = RequestTool.sendGet(BV, null);
        int index1 = HTML.indexOf("<script>window.__playinfo__");
        int index2 = HTML.indexOf("</script>", index1);
        String mediaJson = HTML.substring(index1 + 28, index2);
        System.out.println(mediaJson);

        String videoUrl = JsonPath.parse(mediaJson).read("$.data.dash.video[0].baseUrl");
        String videoInitialization = JsonPath.parse(mediaJson).read("$.data.dash.video[0].SegmentBase.Initialization");
        String videoindexRange = JsonPath.parse(mediaJson).read("$.data.dash.video[0].SegmentBase.indexRange");
        System.out.println(videoUrl + "\n" + videoInitialization + "\n" + videoindexRange);
        String audioUrl = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].baseUrl");
        String audioInitialization = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].SegmentBase.Initialization");
        String audioindexRange = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].SegmentBase.indexRange");
        System.out.println(audioUrl + "\n" + audioInitialization + "\n" + audioindexRange);
        System.out.println("开始下载数据");
        //通过 Executors获取一个定长的线程池  定长为3 超过线程池长度时就会等待
        ExecutorService executorService = Executors.newFixedThreadPool(3);//返回一个执行器的服务类

        //使用线程池启动线程
        donwLoad audio = new donwLoad(audioUrl, audioindexRange, BV, "https://www.bilibili.com");
        donwLoad video = new donwLoad(videoUrl, videoindexRange, BV, "https://www.bilibili.com");
        FutureTask<String> audioFutureTask = new FutureTask<String>(audio);
        FutureTask<String> videoFutureTask = new FutureTask<String>(video);
        //获取最大range
        executorService.submit(audioFutureTask);
        executorService.submit(videoFutureTask);
        String s1;
        String s2;
        try {
            s1 = audioFutureTask.get();
            s2 = videoFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("出现异常，结束进程！");
            return;
        }

        long start = System.currentTimeMillis();
        //下载最大range
        writeRun audio1 = new writeRun(audioUrl, "0-" + (Integer.parseInt(s1) - 1), BV, "https://www.bilibili.com", "download/audio.m4s");
        writeRun video1 = new writeRun(videoUrl, "0-" + (Integer.parseInt(s2) - 1), BV, "https://www.bilibili.com", "download/video.m4s");
        //获取最大range
        executorService.execute(audio1);
        executorService.execute(video1);
        executorService.shutdown();

        while (true) {
            if (executorService.isTerminated()) break;
        }
        System.out.println("thread shut down,开始合并");
        long stop = System.currentTimeMillis();
        System.out.println("未用多线程分包下载： " + (stop - start) / 1000 + "秒");
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("download/ffmpeg.exe -i download/video.m4s -i download/audio.m4s -codec copy download/" + bvPath + ".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class donwLoad implements Callable {
    private SocketSpiderTool socketTools;

    public donwLoad(String url, String range, String referer, String Origin) {
        socketTools = new SocketSpiderTool(url);
        socketTools.getHost();
        socketTools.setHeader("Connection", "keep-alive");
        socketTools.setHeader("Origin", Origin);
        socketTools.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        socketTools.setHeader("Accept", "*/*");
        socketTools.setHeader("Referer", referer);
        socketTools.setHeader("Accept-encoding", "gzip, deflate, br");
        socketTools.setHeader("Accept-language", "zh-CN,zh;q=0.9");
        socketTools.setHeader("Range", "bytes=" + range);
    }

    @Override
    public Object call() throws Exception {
        try {
            socketTools.doNotGetBody();
            socketTools.sendGet();
            Map<String, String> responseHeader = socketTools.getResponseHeader();
            //System.out.println(responseHeader);
            if (responseHeader.containsKey("Content-Range")) {
                String range1 = responseHeader.get("Content-Range");
                int i = range1.indexOf("/");
                return range1.substring(i + 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class writeRun implements Runnable {
    private SocketSpiderTool socketTools;
    private String path;

    public writeRun(String url, String range, String referer, String Origin, String path) {
        socketTools = new SocketSpiderTool(url);
        socketTools.getHost();
        socketTools.setHeader("Connection", "keep-alive");
        socketTools.setHeader("Origin", Origin);
        socketTools.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        socketTools.setHeader("Accept", "*/*");
        socketTools.setHeader("Referer", referer);
        socketTools.setHeader("Accept-encoding", "gzip, deflate, br");
        socketTools.setHeader("Accept-language", "zh-CN,zh;q=0.9");
        socketTools.setHeader("Range", "bytes=" + range);
        this.path = path;
    }

    @Override
    public void run() {
        try {
            long st = System.currentTimeMillis();
            socketTools.sendGet();
            long sto = System.currentTimeMillis();
            System.out.println("下载一个数据块： " + (sto - st) / 1000 + "秒");
            //System.out.println("开始写入：" + this.path);
            socketTools.writeToFile(this.path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




