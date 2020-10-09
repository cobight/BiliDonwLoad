package cn.cobight.Util;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SpiderCarry
 * description:
 * author:cobight
 * createTime:2020/10/8 9:23
 * version:1.0.0
 */

public class SpiderCarry {


    private SpiderRequest request;
    private SpiderResponse response;
    private boolean isChunked = false;

    public SpiderCarry(SpiderRequest request, SpiderResponse response) {
        if (request==null || response==null) throw new NullPointerException("variable request or response is null");
        this.request = request;
        this.response = response;
    }

    /*
     * @description: 创建socket模拟发送get请求
     * @author: cobight
     * @date: 2020/9/5
     * @return: void
     */
    public void sendGet() throws IOException {
        final Socket socket;
        if (request.isHttps) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(request.host, 443);
        } else {
            socket = new Socket(request.host, 80);
        }
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader("GET").getBytes();
        outputStream.write(bytes);
//        if (reqBody != null){//post
//            outputStream.write(reqBody);
//        }
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        response.byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[2048];
        int len = 0;
        byte[] bytes1;
        bytes1 = myReadLine(inputStream);
        //一行一行读头
        if (bytes1 != null) getResponseCodeAndState(new String(bytes1));
        while ((bytes1 = myReadLine(inputStream)).length > 0) {
            System.out.println("head:" + new String(bytes1));
            getResponseHeader(new String(bytes1));
        }
        //读身体
        if (request.getBody){
            while ((len = inputStream.read(temp)) != -1) {
                response.byteArrayOutputStream.write(temp, 0, len);
                response.byteArrayOutputStream.flush();
            }
        }
        response.byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();

    }

    /*SENDPOST*/
    public void sendPost() throws IOException {
        Socket socket = null;
        if (request.isHttps) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(request.host, 443);
        } else {
            socket = new Socket(request.host, 80);
        }
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader("POST").getBytes();
        outputStream.write(bytes);
        if (request.reqBody != null) {//post
            outputStream.write(request.reqBody);
        }
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        response.byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[2048];
        int len = 0;
        byte[] bytes1;
        bytes1 = myReadLine(inputStream);
        //一行一行读头
        if (bytes1 != null) getResponseCodeAndState(new String(bytes1));
        while ((bytes1 = myReadLine(inputStream)).length > 0) {
            System.out.println("head:" + new String(bytes1));
            getResponseHeader(new String(bytes1));
        }
        //读身体
        while ((len = inputStream.read(temp)) != -1) {
            if (request.getBody) {
                response.byteArrayOutputStream.write(temp, 0, len);
                response.byteArrayOutputStream.flush();
            } else {
                break;
            }
        }
        response.byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    public void sendPostChunk() throws IOException {
        Socket socket = null;
        if (request.isHttps) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(request.host, 443);
        } else {
            socket = new Socket(request.host, 80);
        }
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader("POST").getBytes();
        outputStream.write(bytes);
        if (request.reqBody != null) {//post
            outputStream.write(request.reqBody);
        }
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        response.byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[65536];
        int len = 0;
        byte[] bytes1;
        bytes1 = myReadLine(inputStream);
        //一行一行读头
        if (bytes1 != null) getResponseCodeAndState(new String(bytes1));
        while ((bytes1 = myReadLine(inputStream)).length > 0) {
            System.out.println("head:" + new String(bytes1));
            getResponseHeader(new String(bytes1));
        }
        System.out.println("read body");
        if (request.getBody) {
            if (isChunked) {
                while ((bytes1 = myReadLine(inputStream)) != null) {
                    if (bytes1.length == 0) break;
                    System.out.println("chunk head:" + new String(bytes1));
                    bytes1 = myReadLine(inputStream);
                    if (bytes1 == null || bytes1.length == 0) break;
                    System.out.println("chunk body:" + new String(bytes1));
                    response.byteArrayOutputStream.write(bytes1);

                }
            } else {
                while ((len = inputStream.read(temp)) != -1) {
                    response.byteArrayOutputStream.write(temp, 0, len);
                    response.byteArrayOutputStream.flush();
                }
            }
        }
        response.byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    /*获取第一行响应头*/
    private void getResponseCodeAndState(String line) {
        if (line == null) return;
        String[] strings = line.split(" ");
        response.responseCode = strings[1];
        response.responseState = strings[2];
    }

    private byte[] myReadLine(InputStream in) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            for (; ; ) {
                int read = in.read();
                if (read != 13) {
                    stream.write(read);
                } else {
                    if (in.read() == 10) {
                        return stream.toByteArray();
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /*获取响应头*/
    private void getResponseHeader(String text) {
        if (text == null) return;
        int index = text.indexOf(":");
        String k = text.substring(0, index);//好像都是首字母大写了，Set-Cookie时设置个Cookie首字母大写应该没事
        /*   [： ]  扔掉*/
        String v = text.substring(index + 2);
        //System.out.println(k + ":" + v);
        if ("Transfer-Encoding".equals(k) && "chunked".equals(v)) {
            isChunked = true;
        }
        if ("Set-Cookie".equals(k)) {//Set-Cookie 不会与Cookie 撞衫
            String value = v.split(";")[0];
            response.responseHeader.put("Cookie", response.responseHeader.containsKey("Cookie") ? response.responseHeader.get("Cookie") + "; " + value : value);
        } else {
            response.responseHeader.put(k, v);
        }
    }
    /*获取请求头，把map中存放的数据以字符串形式返回*/
    private String getRequestHeader(String type) {
        StringBuilder head = new StringBuilder(type+" " + request.getUri() + " HTTP/1.1\r\n");
        Iterator<String> iterator = request.headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            head.append(key).append(": ").append(request.headers.get(key)).append("\r\n");
        }
        head.append("\r\n");
        return head.toString();
    }
}
