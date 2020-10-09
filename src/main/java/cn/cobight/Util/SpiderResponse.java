package cn.cobight.Util;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * fileName:SpiderResponse
 * description:
 * author:cobight
 * createTime:2020/10/8 9:24
 * version:1.0.0
 */
public class SpiderResponse {
    public String responseCode;//200
    public String responseState;//ok
    public Map<String, String> responseHeader = new LinkedHashMap<>();//响应头map
    public ByteArrayOutputStream byteArrayOutputStream;//响应体的字节保存
    /*获取响应头*/
    public Map<String, String> getResponseHeader() {
        return this.responseHeader;
    }

    /*获取响应体：无编码字符串*/
    public String getResponseBody() {
        return this.byteArrayOutputStream.toString();
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
