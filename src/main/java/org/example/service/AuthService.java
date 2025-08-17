package org.example.service;

import org.example.security.HiSecEngine;
import org.example.model.DeviceAuthResult;
import org.example.model.DeviceCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OpenHarmony设备统一管控平台 - 设备认证服务
 * 实现基于国密SM4的设备证书校验功能
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // 认证结果缓存，避免重复验证
    private final ConcurrentHashMap<String, DeviceAuthResult> authCache = new ConcurrentHashMap<>();

    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRE_TIME = TimeUnit.HOURS.toMillis(1);

    // HiSec安全引擎实例
    private final HiSecEngine hiSecEngine;

    public AuthService() {
        this.hiSecEngine = HiSecEngine.getInstance();
    }

    /**
     * 验证设备证书
     * @param deviceId 设备ID
     * @param certStream 证书数据流
     * @return 认证结果
     */
    public DeviceAuthResult verifyDeviceCertificate(String deviceId, InputStream certStream) {
        logger.info("开始验证设备证书，设备ID: {}", deviceId);

        // 检查缓存
        DeviceAuthResult cachedResult = getCachedAuthResult(deviceId);
        if (cachedResult != null && !cachedResult.isExpired()) {
            logger.info("使用缓存的认证结果，设备ID: {}", deviceId);
            return cachedResult;
        }

        try {
            // 1. 解析证书
            DeviceCertificate deviceCert = parseCertificate(certStream);
            if (deviceCert == null) {
                return createFailedResult(deviceId, "证书解析失败");
            }

            // 2. 基础证书有效性检查
            if (!isBasicCertValid(deviceCert)) {
                return createFailedResult(deviceId, "证书基础验证失败");
            }

            // 3. 调用HiSec引擎进行SM4证书校验
            boolean verifyResult = hiSecEngine.verifyDeviceCert(deviceId, certStream);

            if (verifyResult) {
                DeviceAuthResult successResult = createSuccessResult(deviceId, deviceCert);
                // 缓存认证结果
                cacheAuthResult(deviceId, successResult);
                logger.info("设备证书验证成功，设备ID: {}", deviceId);
                return successResult;
            } else {
                return createFailedResult(deviceId, "SM4证书校验失败");
            }

        } catch (Exception e) {
            logger.error("设备证书验证异常，设备ID: {}, 错误: {}", deviceId, e.getMessage(), e);
            return createFailedResult(deviceId, "证书验证异常: " + e.getMessage());
        }
    }

    /**
     * 批量验证设备证书
     * @param deviceCerts 设备证书映射 (deviceId -> certStream)
     * @return 验证结果映射
     */
    public ConcurrentHashMap<String, DeviceAuthResult> batchVerifyDeviceCertificates(
            ConcurrentHashMap<String, InputStream> deviceCerts) {

        logger.info("开始批量验证设备证书，设备数量: {}", deviceCerts.size());

        ConcurrentHashMap<String, DeviceAuthResult> results = new ConcurrentHashMap<>();

        // 并行处理提高效率
        deviceCerts.entrySet().parallelStream().forEach(entry -> {
            String deviceId = entry.getKey();
            InputStream certStream = entry.getValue();
            DeviceAuthResult result = verifyDeviceCertificate(deviceId, certStream);
            results.put(deviceId, result);
        });

        logger.info("批量证书验证完成，成功: {}, 失败: {}",
                results.values().stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum(),
                results.values().stream().mapToLong(r -> r.isSuccess() ? 0 : 1).sum());

        return results;
    }

    /**
     * 解析设备证书
     */
    private DeviceCertificate parseCertificate(InputStream certStream) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(certStream);

            DeviceCertificate deviceCert = new DeviceCertificate();
            deviceCert.setSubject(x509Cert.getSubjectDN().getName());
            deviceCert.setIssuer(x509Cert.getIssuerDN().getName());
            deviceCert.setSerialNumber(x509Cert.getSerialNumber().toString());
            deviceCert.setNotBefore(x509Cert.getNotBefore());
            deviceCert.setNotAfter(x509Cert.getNotAfter());
            deviceCert.setX509Certificate(x509Cert);

            return deviceCert;

        } catch (CertificateException e) {
            logger.error("证书解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 基础证书有效性检查
     */
    private boolean isBasicCertValid(DeviceCertificate cert) {
        try {
            // 检查证书是否过期
            cert.getX509Certificate().checkValidity();

            // 检查证书主题是否包含设备信息
            String subject = cert.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                logger.warn("证书主题为空");
                return false;
            }

            // 可以添加更多的基础验证逻辑
            // 例如：检查证书链、CRL等

            return true;

        } catch (Exception e) {
            logger.error("基础证书验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建成功的认证结果
     */
    private DeviceAuthResult createSuccessResult(String deviceId, DeviceCertificate cert) {
        DeviceAuthResult result = new DeviceAuthResult();
        result.setDeviceId(deviceId);
        result.setSuccess(true);
        result.setMessage("证书验证成功");
        result.setTimestamp(System.currentTimeMillis());
        result.setCertificate(cert);
        result.setAuthMethod("SM4");
        return result;
    }

    /**
     * 创建失败的认证结果
     */
    private DeviceAuthResult createFailedResult(String deviceId, String message) {
        DeviceAuthResult result = new DeviceAuthResult();
        result.setDeviceId(deviceId);
        result.setSuccess(false);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        result.setAuthMethod("SM4");
        return result;
    }

    /**
     * 获取缓存的认证结果
     */
    private DeviceAuthResult getCachedAuthResult(String deviceId) {
        DeviceAuthResult cached = authCache.get(deviceId);
        if (cached != null && (System.currentTimeMillis() - cached.getTimestamp()) > CACHE_EXPIRE_TIME) {
            authCache.remove(deviceId);
            return null;
        }
        return cached;
    }

    /**
     * 缓存认证结果
     */
    private void cacheAuthResult(String deviceId, DeviceAuthResult result) {
        authCache.put(deviceId, result);
    }

    /**
     * 清理过期的缓存
     */
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        authCache.entrySet().removeIf(entry ->
                (currentTime - entry.getValue().getTimestamp()) > CACHE_EXPIRE_TIME);
    }

    /**
     * 获取认证统计信息
     */
    public String getAuthStatistics() {
        long totalCached = authCache.size();
        long successCount = authCache.values().stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failedCount = totalCached - successCount;

        return String.format("认证缓存统计 - 总计: %d, 成功: %d, 失败: %d",
                totalCached, successCount, failedCount);
    }
}