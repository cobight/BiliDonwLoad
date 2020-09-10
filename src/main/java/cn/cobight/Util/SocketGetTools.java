package cn.cobight.Util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SocketGetTools
 * description: 通过socket模拟浏览器向服务端收发数据
 * author:cobight
 * createTime:2020/9/6 14:15
 * version:1.0.0
 */
public class SocketGetTools {
    private String url;//传入的网址
    private String host;//截取传入网址的开头host
    private String param;//截取传入网址的参数部分
    private boolean getBody = true;//是否只获取响应头，默认为true则获取响应体
    private Map<String, String> headers = new LinkedHashMap<>();//请求头map
    private Map<String, String> responseHeader = new LinkedHashMap<>();//响应头map
    private ByteArrayOutputStream byteArrayOutputStream;//响应体的字节保存
    /*构造方法*/
    public SocketGetTools(String url) {
        if (url.substring(0, 5).equals("https")) {
            this.url = url.substring(0, 4) + url.substring(5);
        } else {
            this.url = url;
        }
        //initHeaders();
    }

    public SocketGetTools(String url, String param) {
        if (url.substring(0, 5).equals("https")) {
            this.url = url.substring(0, 4) + url.substring(5);
        } else {
            this.url = url;
        }
        if (param != null) {
            if (this.url.contains("?")) {
                this.url += "&" + param;
            } else {
                this.url += "?" + param;
            }
        }
        this.param = param;
        //initHeaders();
    }
    /*设置头*/
    public void setHeader(String key, String value) {
        if (key != null && value != null) {
            headers.put(key, value);
        }
    }
    /*只获取响应头，在sendGet前调用*/
    public void doNotGetBody(){
        this.getBody = false;
    }
    /*原本想初始化user-agent和host，最后为了linkedhashmap不写了，就想看看顺序而已，想快那就hashmap*/
    private void initHeaders() {
        setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        setHeader("Accept", "*/*");

    }
    /*为了灵活性，可以手动控制是否加host*/
    public void getHost(){
        int i = this.url.indexOf("/", 9);
        this.host = this.url.substring(7,i);
        //System.out.println(this.host+"----------");
        this.headers.put("Host", this.host);
    }
    /*截取.com/.cn 后面的部分，为请求报文第一行数据做准备   GET /test.html HTTP/1.1*/
    private String getUri(){
        int i = url.indexOf("/",9);
        return url.substring(i);
    }
    /*
     * @description: 创建socket模拟发送get请求
     * @author: cobight
     * @date: 2020/9/5
     * @return: void
     */
    public void sendGet() throws IOException {
        InetSocketAddress isa = new InetSocketAddress(this.host, 80);
        Socket socket = new Socket();
        socket.connect(isa);
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader().getBytes();
        outputStream.write(bytes);
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp=new byte[2048];
        int len=0;
        boolean head = true;
        while((len = inputStream.read(temp)) != -1) {
            if (new String(temp,0,len).contains("\r\n\r\n")) {/*\r\n\r\n是响应头与响应体的分界线，若链接没有响应体，下个while将不满足循环条件*/
                if (head) {
                    getResponseHeader(new String(temp, 0, len));
                    System.out.println("SocketGetTools --> sendGet: " + this.responseHeader.get("state"));
                    head = false;
                    continue;
                }
            }
            if (getBody){
                byteArrayOutputStream.write(temp,0,len);
                byteArrayOutputStream.flush();
            }else {
                break;
            }
        }
        byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }
    /*获取响应头*/
    private Map<String, String> getResponseHeader(String text){
        if (text==null)return null;
        //System.out.println(text);
//        System.out.println(Arrays.toString(text.split("\r\n")));
        String[] splits = text.split("\r\n");
        responseHeader.put("state",splits[0]);
        for (int i = 1; i < splits.length; i++) {
            String line = splits[i];
            int index = line.indexOf(":");
            //System.out.println(line.substring(0,index)+"\t"+line.substring(index+2));
            responseHeader.put(line.substring(0,index),line.substring(index+2));
        }
        return responseHeader;
    }
    /*获取请求头，把map中存放的数据以字符串形式返回*/
    private String getRequestHeader(){
        StringBuilder head = new StringBuilder("GET " + getUri() + " HTTP/1.1\r\n");
        Iterator<String> iterator = headers.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            head.append(key).append(": ").append(headers.get(key)).append("\r\n");
        }
        head.append("\r\n");
        //System.out.println(head.toString());
        return head.toString();
    }
    /*获取响应头*/
    public Map<String, String> getResponseHeader(){
        return this.responseHeader;
    }
    /*获取响应体：无编码字符串*/
    public String getResponseBody(){
        return byteArrayOutputStream.toString();
    }
    /*获取响应体：编码字符串*/
    public String getResponseBody(String charsetName) {
        try {
            return byteArrayOutputStream.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    /*获取响应体：字节*/
    public byte[] getResponseBody_bytes(){
        return byteArrayOutputStream.toByteArray();
    }
    /*获取存放响应体的流*/
    public ByteArrayOutputStream getByteArrayOutputStream(){
        return this.byteArrayOutputStream;
    }
    /*输出到 输出流*/
    public void writeTo(OutputStream outputStream){
        try {
            byteArrayOutputStream.writeTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*输出到 文件*/
    public void writeToFile(String path){
        try {
            this.byteArrayOutputStream.writeTo(new FileOutputStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
