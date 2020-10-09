package cn.cobight.Util;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SocketSpiderTool
 * description:
 * author:cobight
 * createTime:2020/9/23 22:17
 * version:1.0.0
 */
public class SocketSpiderTool {
    private String url;//传入的网址
    private String host;//截取传入网址的开头host
    private String param;//截取传入网址的参数部分
    private byte[] reqBody;
    private volatile boolean getBody = true;//是否只获取响应头，默认为true则获取响应体
    private String responseCode;//200
    private String responseState;//ok
    private boolean isChunked = false;
    private volatile Map<String, String> headers = new LinkedHashMap<>();//请求头map
    private volatile Map<String, String> responseHeader = new LinkedHashMap<>();//响应头map
    private volatile ByteArrayOutputStream byteArrayOutputStream;//响应体的字节保存

    /*构造方法*/
    public SocketSpiderTool(String url) {
        this.url = url;
        //initHeaders();
    }

    public SocketSpiderTool(String url, String param) {
        this.url = url;
        if (param != null) {
            if (this.url.contains("?")) {
                this.url += param;
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

    public void removeHeader(String key) {
        if (this.headers.containsKey(key)) {
            this.headers.remove(key);
        }
    }

    /*只获取响应头，在sendGet前调用*/
    public void doNotGetBody() {
        this.getBody = false;
    }

    /*原本想初始化user-agent和host，最后为了linkedhashmap不写了，就想看看顺序而已，想快那就hashmap*/
    private void initHeaders() {
        setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        setHeader("Accept", "*/*");

    }

    /*为了灵活性，可以手动控制是否加host*/
    public void getHost() {
        int i = this.url.indexOf("/", 9);
        if (this.url.substring(0, 5).equals("https")) {
            this.host = this.url.substring(8, i);
        } else {
            this.host = this.url.substring(7, i);
        }


        this.headers.put("Host", this.host);
    }

    /*截取.com/.cn 后面的部分，为请求报文第一行数据做准备   GET /test.html HTTP/1.1*/
    private String getUri() {
        int i = url.indexOf("/", 9);
        return url.substring(i);
    }

    public void setRequestBody(byte[] body) {
        reqBody = body;
    }

    public void setRequestBody(String body) {
        reqBody = body.getBytes();
    }

    /*
     * @description: 创建socket模拟发送get请求
     * @author: cobight
     * @date: 2020/9/5
     * @return: void
     */
    public void sendGet() throws IOException {
        final Socket socket;
        if (this.url.substring(0, 5).equals("https")) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(this.host, 443);
        } else {
            socket = new Socket(this.host, 80);
        }
        final OutputStream outputStream = socket.getOutputStream();
        final InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader().getBytes();
        outputStream.write(bytes);
//        if (reqBody != null){//post
//            outputStream.write(reqBody);
//        }
        outputStream.flush();
        System.out.println("start read");
        long st = System.currentTimeMillis();
        //准备存放响应体的字节流
        byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[2048];
        int len = 0;
        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        //一行一行读头
        getResponseCodeAndState(reader.readLine());
        while (!(line = reader.readLine()).equals("")) {
            getResponseHeader(line);
        }
        //读身体
        if (getBody){
            while ((len = inputStream.read(temp)) != -1) {
                byteArrayOutputStream.write(temp, 0, len);
                byteArrayOutputStream.flush();
            }
        }
        long sto = System.currentTimeMillis();
        System.out.println("下载一个body： " + (sto - st) / 1000 + "秒");
        byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    /*SENDPOST*/
    public synchronized void sendPost() throws IOException {
        Socket socket = null;
        if (this.url.substring(0, 5).equals("https")) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(this.host, 443);
        } else {
            socket = new Socket(this.host, 80);
        }
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader().getBytes();
        outputStream.write(bytes);
        if (reqBody != null) {//post
            outputStream.write(reqBody);
        }
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[2048];
        int len = 0;
        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        //一行一行读头
        getResponseCodeAndState(reader.readLine());
        while (!(line = reader.readLine()).equals("")) {
            getResponseHeader(line);
        }
        //读身体
        while ((len = inputStream.read(temp)) != -1) {
            if (getBody) {
                byteArrayOutputStream.write(temp, 0, len);
                byteArrayOutputStream.flush();
            } else {
                break;
            }
        }
        byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    public void sendPostChunk() throws IOException {
        Socket socket = null;
        if (this.url.substring(0, 5).equals("https")) {
            socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(this.host, 443);
        } else {
            socket = new Socket(this.host, 80);
        }
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        //以字节的形式发送请求头
        byte[] bytes = getRequestHeader().getBytes();
        outputStream.write(bytes);
        if (reqBody != null) {//post
            outputStream.write(reqBody);
        }
        outputStream.flush();
        System.out.println("start read");
        //准备存放响应体的字节流
        byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp = new byte[65536];
        int len = 0;
        String line = null;

        byte[] bytes1;
        bytes1 = myReadLine(inputStream);
        //一行一行读头
        if (bytes1 != null) getResponseCodeAndState(new String(bytes1));
        while ((bytes1 = myReadLine(inputStream)).length > 0) {
            System.out.println("head:" + new String(bytes1));
            getResponseHeader(new String(bytes1));
        }
        System.out.println("");
        if (getBody) {
            if (isChunked) {
                while ((bytes1 = myReadLine(inputStream)) != null) {
                    if (bytes1.length == 0) break;
                    System.out.println("chunk head:" + new String(bytes1));
                    bytes1 = myReadLine(inputStream);
                    if (bytes1 == null || bytes1.length == 0) break;
                    System.out.println("chunk body:" + new String(bytes1));
                    byteArrayOutputStream.write(bytes1);

                }
            } else {
                while ((len = inputStream.read(temp)) != -1) {
                    byteArrayOutputStream.write(temp, 0, len);
                    byteArrayOutputStream.flush();
                }
            }
        }
        byteArrayOutputStream.close();
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    /*获取第一行响应头*/
    private void getResponseCodeAndState(String line) {
        if (line == null) return;
        String[] strings = line.split(" ");
        responseCode = strings[1];
        responseState = strings[2];
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
            responseHeader.put("Cookie", responseHeader.containsKey("Cookie") ? responseHeader.get("Cookie") + "; " + value : value);
        } else {
            responseHeader.put(k, v);
        }
    }

    /*获取请求头，把map中存放的数据以字符串形式返回*/
    private String getRequestHeader() {
        StringBuilder head = new StringBuilder("GET " + getUri() + " HTTP/1.1\r\n");
        Iterator<String> iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            head.append(key).append(": ").append(headers.get(key)).append("\r\n");
        }
        head.append("\r\n");
        return head.toString();
    }

    private String postRequestHeader() {
        StringBuilder head = new StringBuilder("POST " + getUri() + " HTTP/1.1\r\n");
        Iterator<String> iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            head.append(key).append(": ").append(headers.get(key)).append("\r\n");
        }
        head.append("\r\n");
        return head.toString();
    }

    /*获取响应头*/
    public Map<String, String> getResponseHeader() {
        return this.responseHeader;
    }

    /*获取响应体：无编码字符串*/
    public String getResponseBody() {
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
    public byte[] getResponseBody_bytes() {
        return byteArrayOutputStream.toByteArray();
    }

    /*获取存放响应体的流*/
    public ByteArrayOutputStream getByteArrayOutputStream() {
        return this.byteArrayOutputStream;
    }

    /*输出到 输出流*/
    public void writeTo(OutputStream outputStream) {
        try {
            byteArrayOutputStream.writeTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*输出到 文件*/
    public void writeToFile(String path) {
        try {/*B站的路径可能有？  ， 所以还是防止路径错误，整一下*/
            this.byteArrayOutputStream.writeTo(new FileOutputStream(path.replace("?", "_")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
