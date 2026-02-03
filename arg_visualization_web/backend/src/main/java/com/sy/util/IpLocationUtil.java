package com.sy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * IP地理位置解析工具类
 * 使用免费的IP地理位置API服务（ip-api.com 支持 lang=zh-CN / lang=en）
 */
@Slf4j
@Component
public class IpLocationUtil {

    private static final String IP_API_URL = "http://ip-api.com/json/%s?lang=%s&fields=status,message,country,regionName,city";
    private static final String IP_API_SERVER = "http://ip-api.com/json/?lang=%s&fields=status,message,country,regionName,city,query";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 判断是否为内网IP
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        // IPv6本地地址
        if (ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        // IPv4本地地址
        if (ip.equals("127.0.0.1") || ip.equals("localhost")) {
            return true;
        }
        // 内网IP段：10.x.x.x, 172.16-31.x.x, 192.168.x.x
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                int secondOctet = Integer.parseInt(ip.split("\\.")[1]);
                if (secondOctet >= 16 && secondOctet <= 31) {
                    return true;
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return false;
    }
    
    /**
     * 是否为中国地区（含港澳），用于拼写格式判断
     */
    private boolean isChinaRegion(String country) {
        if (country == null) return false;
        String c = country.trim();
        return "中国".equals(c) || "China".equals(c)
            || "香港".equals(c) || "Hong Kong".equals(c)
            || "澳门".equals(c) || "Macau".equals(c) || "Macao".equals(c)
            || c.startsWith("中国香港") || c.startsWith("中国澳门");
    }

    /**
     * 根据 country/region/city 和语言拼出展示串
     * zh-CN：中国=省+市，其他=国家+空格+州+空格+城市；en：中国=城市, 州，其他=城市, 州, 国家
     */
    private String buildLocationString(String country, String region, String city, String lang) {
        boolean isChina = isChinaRegion(country);
        if ("en".equals(lang)) {
            if (isChina) {
                if (region != null && !region.isEmpty() && city != null && !city.isEmpty()) {
                    return city + ", " + region;
                }
                if (city != null && !city.isEmpty()) return city;
                if (region != null && !region.isEmpty()) return region;
                return country != null ? country : "Unknown";
            } else {
                StringBuilder sb = new StringBuilder();
                if (city != null && !city.isEmpty()) sb.append(city);
                if (region != null && !region.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(region);
                }
                if (country != null && !country.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(country);
                }
                return sb.length() > 0 ? sb.toString() : "Unknown";
            }
        }
        // zh-CN
        if (isChina) {
            StringBuilder sb = new StringBuilder();
            if (region != null && !region.isEmpty()) sb.append(region);
            if (city != null && !city.isEmpty()) sb.append(city);
            return sb.length() > 0 ? sb.toString() : (country != null ? country : "未知");
        } else {
            StringBuilder sb = new StringBuilder();
            if (country != null && !country.isEmpty()) sb.append(country);
            if (region != null && !region.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(region);
            }
            if (city != null && !city.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(city);
            }
            return sb.length() > 0 ? sb.toString() : "未知";
        }
    }

    /**
     * 按指定语言获取 IP 对应地理位置（单次请求）
     */
    private String getLocationByIpWithLang(String ip, String lang) {
        try {
            String url = String.format(IP_API_URL, ip, lang);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) return null;
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            JsonNode jsonNode = objectMapper.readTree(response.toString());
            if (!"success".equals(jsonNode.has("status") ? jsonNode.get("status").asText() : "")) return null;
            String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";
            String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
            String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
            return buildLocationString(country, region, city, lang);
        } catch (Exception e) {
            log.error("获取IP地理位置失败: IP={}, lang={}, 错误={}", ip, lang, e.getMessage());
            return null;
        }
    }

    private String getLocationByServerOutboundIpWithLang(String lang) {
        try {
            String url = String.format(IP_API_SERVER, lang);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) return null;
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            JsonNode jsonNode = objectMapper.readTree(response.toString());
            if (!"success".equals(jsonNode.has("status") ? jsonNode.get("status").asText() : "")) return null;
            String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";
            String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
            String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
            return buildLocationString(country, region, city, lang);
        } catch (Exception e) {
            log.warn("获取服务器出口IP地理位置失败（内网/无外网时正常）: lang={}, 错误={}", lang, e.getMessage());
            return null;
        }
    }

    /**
     * 根据IP地址获取地理位置信息（中文，兼容旧调用）
     */
    public String getLocationByIp(String ip) {
        if (isPrivateIp(ip)) {
            log.info("检测到内网IP: {}，尝试获取服务器出口IP进行定位", ip);
            return getLocationByServerOutboundIpWithLang("zh-CN");
        }
        return getLocationByIpWithLang(ip, "zh-CN");
    }

    /**
     * 根据IP获取中英双语言地理位置，用于写入 DB 的 location 字段（JSON 字符串）
     * 格式：{"zh":"浙江省嘉兴市","en":"Jiaxing, Zhejiang"}
     */
    public Map<String, String> getLocationByIpBoth(String ip) {
        Map<String, String> result = new LinkedHashMap<>();
        String zh;
        String en;
        if (isPrivateIp(ip)) {
            log.info("检测到内网IP: {}，尝试获取服务器出口IP进行定位（失败则记为未知）", ip);
            CompletableFuture<String> zhFuture = CompletableFuture.supplyAsync(() -> getLocationByServerOutboundIpWithLang("zh-CN"));
            CompletableFuture<String> enFuture = CompletableFuture.supplyAsync(() -> getLocationByServerOutboundIpWithLang("en"));
            zh = zhFuture.join();
            en = enFuture.join();
        } else {
            zh = getLocationByIpWithLang(ip, "zh-CN");
            en = getLocationByIpWithLang(ip, "en");
        }
        // 某一语言失败时重试一次（常为超时或偶发失败）
        if (zh == null && isPrivateIp(ip)) zh = getLocationByServerOutboundIpWithLang("zh-CN");
        else if (zh == null) zh = getLocationByIpWithLang(ip, "zh-CN");
        if (en == null && isPrivateIp(ip)) en = getLocationByServerOutboundIpWithLang("en");
        else if (en == null) en = getLocationByIpWithLang(ip, "en");
        result.put("zh", zh != null ? zh : "未知");
        result.put("en", en != null ? en : "Unknown");
        // 仍失败则用另一语言回退，避免出现「未知」+ 正常英文
        if (zh == null && en != null) result.put("zh", result.get("en"));
        if (en == null && zh != null) result.put("en", result.get("zh"));
        return result;
    }

    /**
     * 返回中英双语言地理位置的 JSON 字符串，直接写入 login_logs.location
     */
    public String getLocationByIpBothJson(String ip) {
        Map<String, String> both = getLocationByIpBoth(ip);
        try {
            return objectMapper.writeValueAsString(both);
        } catch (Exception e) {
            log.warn("序列化 location JSON 失败: {}", e.getMessage());
            return "{\"zh\":\"未知\",\"en\":\"Unknown\"}";
        }
    }

    /**
     * 通过服务器出口IP获取地理位置（用于内网访问时，兼容旧调用）
     */
    private String getLocationByServerOutboundIp() {
        return getLocationByServerOutboundIpWithLang("zh-CN");
    }
}

