package cn.cobight.Util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SpiderRequest
 * description:
 * author:cobight
 * createTime:2020/10/8 9:24
 * version:1.0.0
 */
public class SpiderRequest {
    public String url;//传入的网址
    public String host;//截取传入网址的开头host
    public String param;//截取传入网址的参数部分
    public byte[] reqBody;
    public Map<String, String> headers = new LinkedHashMap<>();//请求头map
    public boolean isHttps;
    public boolean getBody = true;//是否只获取响应头，默认为true则获取响应体
    /*构造方法*/
    public SpiderRequest(String url) {
        this.url = url;
        initHeaders();
    }

    public SpiderRequest(String url, String param) {
        this.url = url;
        if (param != null) {
            this.url += this.url.contains("?")?param:"?"+param;
//            if (this.url.contains("?")) {
//                this.url += param;
//            } else {
//                this.url += "?" + param;
//            }
        }
        this.param = param;
        initHeaders();
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
    public void initHeaders() {
        setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4209.400");
        //setHeader("Accept", "*/*");
        setHost();
    }

    /*为了灵活性，可以手动控制是否加host*/
    public void setHost() {
        int i = this.url.indexOf("/", 9);
        if (this.url.substring(0, 5).equals("https")) {
            this.host = this.url.substring(8, i);
            this.isHttps = true;
        } else {
            this.host = this.url.substring(7, i);
        }
        this.headers.put("Host", this.host);
    }

    /*截取.com/.cn 后面的部分，为请求报文第一行数据做准备   GET /test.html HTTP/1.1*/
    public String getUri() {
        int i = this.url.indexOf("/", 9);
        return this.url.substring(i);
    }

    public void setRequestBody(byte[] body) {
        reqBody = body;
    }

    public void setRequestBody(String body) {
        reqBody = body.getBytes();
    }
}
