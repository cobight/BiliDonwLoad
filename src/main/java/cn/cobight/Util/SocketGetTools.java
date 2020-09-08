package cn.cobight.Util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SocketGetTools
 * description:
 * author:cobight
 * createTime:2020/9/6 14:15
 * version:1.0.0
 */
public class SocketGetTools {
    private String url;
    private String host;
    private String param;
    private boolean getBody = true;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> responseHeader = new LinkedHashMap<>();
    private ByteArrayOutputStream byteArrayOutputStream;
    private String name;
    public void setName(String name){
        this.name = name;
    }
    public SocketGetTools(String url) {
        if (url.substring(0, 5).equals("https")) {
            this.url = url.substring(0, 4) + url.substring(5);
        } else {
            this.url = url;
        }
        initHeaders();
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
        initHeaders();
    }

    public void setHeader(String key, String value) {
        if (key != null && value != null) {
            headers.put(key, value);
        }
    }
    public void doNotGetBody(){
        this.getBody = false;
    }
    private void initHeaders() {


    }
    public void getHost(){
        int i = this.url.indexOf("/", 9);
        this.host = this.url.substring(7,i);
        //System.out.println(this.host+"----------");
        this.headers.put("Host", this.host);
    }
    private String getUri(){
        int i = url.indexOf("/",9);
        return url.substring(i);
    }
    /*
     * @description: get
     * @author: cobight
     * @date: 2020/9/5
     * @params:
     * @param :
     * @return: void
     */
    public void sendGet() throws IOException {
        InetSocketAddress isa = new InetSocketAddress(this.host, 80);
        Socket socket = new Socket();
        socket.connect(isa);
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        byte[] bytes = getRequestHeader().getBytes();
        outputStream.write(bytes);
        outputStream.flush();
        System.out.println("start read");
        byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] temp=new byte[2048];
        int len=0;
        boolean head = true;
        while((len = inputStream.read(temp)) != -1) {
            if (new String(temp,0,len).contains("\r\n\r\n")) {
                if (head) {
                    getResponseHeader(new String(temp, 0, len));
                    System.out.println(this.responseHeader.get("state"));
                    head = false;
                    continue;
                }
            }
            if (getBody){
//                if (this.name!= null){
//                    System.out.println(this.name);
//                }
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
    public Map<String, String> getResponseHeader(){
        return this.responseHeader;
    }
    public String getResponseBody(){
        return byteArrayOutputStream.toString();
    }
    public String getResponseBody(String charsetName) {
        try {
            return byteArrayOutputStream.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    public byte[] getResponseBody_bytes(){
        return byteArrayOutputStream.toByteArray();
    }
    public ByteArrayOutputStream getByteArrayOutputStream(){
        return this.byteArrayOutputStream;
    }
    public void writeTo(OutputStream outputStream){
        try {
            byteArrayOutputStream.writeTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeToFile(String path){
        try {
            this.byteArrayOutputStream.writeTo(new FileOutputStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
