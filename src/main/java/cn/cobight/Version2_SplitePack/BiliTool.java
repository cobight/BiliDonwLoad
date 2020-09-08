package cn.cobight.Version2_SplitePack;


import cn.cobight.Util.RequestTool;
import cn.cobight.Util.SocketGetTools;
import com.jayway.jsonpath.JsonPath;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * fileName:BiliTool
 * description: 利用socket模拟浏览器发送请求，下载bili视频并合成
 * 第二个版本，音频与视频的m4s文件拆包，下载好像没提速
 * author:cobight
 * createTime:2020/9/4 14:18
 * version:2.0.0
 */
//@SuppressWarnings("all")
public class BiliTool {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        downbili("BV1o5411873u");

    }

    public static void downbili(String bvPath) throws ExecutionException, InterruptedException, IOException {
        long start = System.currentTimeMillis();
        String BV = "https://www.bilibili.com/video/" + bvPath;
        //第一个请求偷下懒，没用socket
        String HTML = RequestTool.sendGet(BV, null);
        int index1 = HTML.indexOf("<script>window.__playinfo__");
        int index2 = HTML.indexOf("</script>", index1);
        String mediaJson = HTML.substring(index1 + 28, index2);
        System.out.println(mediaJson);
        //解析任务json
        String videoUrl = JsonPath.parse(mediaJson).read("$.data.dash.video[0].baseUrl");
        String videoInitialization = JsonPath.parse(mediaJson).read("$.data.dash.video[0].SegmentBase.Initialization");
        String videoIndexRange = JsonPath.parse(mediaJson).read("$.data.dash.video[0].SegmentBase.indexRange");
        System.out.println(videoUrl + "\n" + videoInitialization + "\n" + videoIndexRange);
        String audioUrl = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].baseUrl");
        String audioInitialization = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].SegmentBase.Initialization");
        String audioIndexRange = JsonPath.parse(mediaJson).read("$.data.dash.audio[0].SegmentBase.indexRange");
        System.out.println(audioUrl + "\n" + audioInitialization + "\n" + audioIndexRange);
        //通过 Executors获取一个定长的线程池  定长为3 超过线程池长度时就会等待
        ExecutorService executorService = Executors.newFixedThreadPool(10);//返回一个执行器的服务类
        //使用线程池启动线程
        donwLoad audio = new donwLoad(audioUrl, audioIndexRange, BV, "https://www.bilibili.com");
        donwLoad video = new donwLoad(videoUrl, videoIndexRange, BV, "https://www.bilibili.com");
        FutureTask<String> audioFutureTask = new FutureTask<String>(audio);
        FutureTask<String> videoFutureTask = new FutureTask<String>(video);
        //获取最大range
        executorService.submit(audioFutureTask);
        executorService.submit(videoFutureTask);
        Integer s1;
        Integer s2;
        try {
            s1 = Integer.parseInt(audioFutureTask.get());
            s2 = Integer.parseInt(videoFutureTask.get());
            System.out.println("audio:" + s1);
            System.out.println("video:" + s2);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("出现异常，无法获取文件总长度，结束进程！");
            return;
        }
        //下载最大range
        downData audio1 = new downData(audioUrl, "0-" + (s1 - 1), BV, "https://www.bilibili.com");
        FutureTask<ByteArrayOutputStream> audioFutureTask1 = new FutureTask<ByteArrayOutputStream>(audio1);
        executorService.execute(audioFutureTask1);
        int packNum = 9;
        int packAvg = s2 / packNum;//平均一块的大小
        ArrayList<FutureTask<ByteArrayOutputStream>> downDataArrayList = new ArrayList<>();
        for (int i = 0; i < packNum - 1; i++) {
            downData downData = new downData(videoUrl, (0 + packAvg * i) + "-" + (packAvg * (i + 1) - 1), BV, "https://www.bilibili.com");
            //downData.setName("video:"+i);
            downDataArrayList.add(new FutureTask<ByteArrayOutputStream>(downData));
        }
        downData downData = new downData(videoUrl, (packAvg * (packNum - 1)) + "-" + (s2 - 1), BV, "https://www.bilibili.com");
        //downData.setName("video:"+5);
        downDataArrayList.add(new FutureTask<ByteArrayOutputStream>(downData));

        for (FutureTask<ByteArrayOutputStream> byteArrayOutputStreamFutureTask : downDataArrayList) {
            executorService.execute(byteArrayOutputStreamFutureTask);
        }
        executorService.shutdown();
        //音频较小，会很快，所以先输出音频
        ByteArrayOutputStream audioByteArrayOutputStream = audioFutureTask1.get();
        audioByteArrayOutputStream.writeTo(new FileOutputStream("download/audio.m4s"));
        long audiotime = System.currentTimeMillis();
        System.out.println("音频： "+(audiotime - start) / 1000 + "秒");
        audioByteArrayOutputStream.flush();
        audioByteArrayOutputStream.close();
        ByteArrayOutputStream videoByteArrayOutputStream = new ByteArrayOutputStream();
        for (FutureTask<ByteArrayOutputStream> byteArrayOutputStreamFutureTask : downDataArrayList) {
            ByteArrayOutputStream byteArrayOutputStream = byteArrayOutputStreamFutureTask.get();
            byte[] bytes = byteArrayOutputStream.toByteArray();
            videoByteArrayOutputStream.write(bytes);
            videoByteArrayOutputStream.flush();
        }
        videoByteArrayOutputStream.writeTo(new FileOutputStream("download/video.m4s"));
        long videotime = System.currentTimeMillis();
        System.out.println("视频： "+(videotime - start) / 1000 + "秒");
        videoByteArrayOutputStream.flush();
        videoByteArrayOutputStream.close();
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("download/ffmpeg.exe -i download/video.m4s -i download/audio.m4s -codec copy download/" + bvPath + ".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stop = System.currentTimeMillis();
        System.out.println("用多线程分包下载： "+(stop - start) / 1000 + "秒");
    }
}

class donwLoad implements Callable {
    private SocketGetTools socketTools;

    public donwLoad(String url, String range, String referer, String Origin) {
        socketTools = new SocketGetTools(url);
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
    public Object call() {
        try {
            socketTools.doNotGetBody();
            socketTools.sendGet();
            Map<String, String> responseHeader = socketTools.getResponseHeader();
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

class downData implements Callable {
    private SocketGetTools socketTools;
    public void setName(String name){
        socketTools.setName(name);
    }
    public downData(String url, String range, String referer, String Origin) {
        socketTools = new SocketGetTools(url);
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
    public Object call() {
        try {
            socketTools.sendGet();
            return socketTools.getByteArrayOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}




