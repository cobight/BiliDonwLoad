package cn.cobight.Util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * fileName:RuquestTool 不好用，Origin头不让添加
 * description:
 * author:cobight
 * createTime:2020/9/4 14:15
 * version:1.0.0
 */
public class RequestTool {
    /**
     * 向指定URL发送GET方法的请求，返回字符串
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            param = param!=null?param:"";
            String urlNameString = url + "?" + param;
            System.out.println(urlNameString);
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3756.400 QQBrowser/10.5.4039.400");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));

            String line;
            while ((line = in.readLine()) != null) {
//                System.out.println(line);
                result.append(line).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + url);
//            e.printStackTrace();
            return sendGet(url, param);
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

/*
* @description:
* @author: cobight
* @date: 2020/9/4
* @params:
 	* @param url:
 	* @param filepath:
* @return: void
*/
    public static void download_file(String url, String filepath,List<Map<String,String>> headers) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性头
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            if (headers!=null && headers.size()>0){
                for (Map<String, String> header : headers) {
                    Iterator<String> iterator = header.keySet().iterator();
                    while (iterator.hasNext()){
                        String key = iterator.next();
                        String value = header.get(key);
                        connection.setRequestProperty(key,value);
                    }
                }
            }
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            Iterator<String> iterator = headerFields.keySet().iterator();
            while (iterator.hasNext()){
                String next = iterator.next();
                System.out.println(next+"-"+headerFields.get(next));
            }
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            is = new BufferedInputStream(connection.getInputStream());
            fos = new FileOutputStream(filepath);

            byte[] temp = new byte[1024];
            int len;
            while ((len = is.read(temp)) != -1) {
                fos.write(temp, 0, len);
            }

        } catch (Exception e) {
            System.out.println("ts切片下载出现异常！开始重新下载：" + url);
//            e.printStackTrace();异常重载
            download_file(url, filepath,headers);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 使用finally块来关闭输入流


    }
}
