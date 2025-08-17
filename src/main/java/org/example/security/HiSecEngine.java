package org.example.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * HiSec安全引擎封装类
 * 提供基于国密SM4算法的设备证书校验功能
 */
public class HiSecEngine {

    private static final Logger logger = LoggerFactory.getLogger(HiSecEngine.class);

    private static volatile HiSecEngine instance;

    // SM4算法常量
    private static final String SM4_ALGORITHM = "SM4";
    private static final String SM4_TRANSFORMATION = "SM4/ECB/PKCS5Padding";

    // 模拟的根证书公钥（实际应该从安全存储中获取）
    private static final String ROOT_CERT_PUBLIC_KEY = "mock_root_public_key";

    private HiSecEngine() {
        initializeSecurityEngine();
    }

    /**
     * 获取HiSecEngine单例实例
     */
    public static HiSecEngine getInstance() {
        if (instance == null) {
            synchronized (HiSecEngine.class) {
                if (instance == null) {
                    instance = new HiSecEngine();
                }
            }
        }
        return instance;
    }

    /**
     * 核心方法：验证设备证书
     * 使用国密SM4算法进行证书校验
     *
     * @param deviceId 设备ID
     * @param certStream 证书数据流
     * @return 验证结果
     */
    public boolean verifyDeviceCert(String deviceId, InputStream certStream) {
        logger.info("HiSec引擎开始验证设备证书，设备ID: {}", deviceId);

        try {
            // 1. 读取证书数据
            byte[] certData = readCertificateData(certStream);
            if (certData == null || certData.length == 0) {
                logger.error("证书数据为空，设备ID: {}", deviceId);
                return false;
            }

            // 2. 提取证书指纹
            String certFingerprint = calculateCertFingerprint(certData);
            logger.debug("证书指纹: {}", certFingerprint);

            // 3. SM4解密证书签名
            boolean signatureValid = verifyCertSignatureWithSM4(certData, deviceId);
            if (!signatureValid) {
                logger.warn("证书签名验证失败，设备ID: {}", deviceId);
                return false;
            }

            // 4. 验证证书链
            boolean chainValid = verifyCertificateChain(certData);
            if (!chainValid) {
                logger.warn("证书链验证失败，设备ID: {}", deviceId);
                return false;
            }

            // 5. 检查设备ID与证书匹配性
            boolean deviceMatched = verifyDeviceIdMatch(deviceId, certData);
            if (!deviceMatched) {
                logger.warn("设备ID与证书不匹配，设备ID: {}", deviceId);
                return false;
            }

            // 6. 检查证书撤销状态（CRL检查）
            boolean notRevoked = checkCertificateRevocationStatus(certFingerprint);
            if (!notRevoked) {
                logger.warn("证书已被撤销，设备ID: {}", deviceId);
                return false;
            }

            logger.info("设备证书验证成功，设备ID: {}", deviceId);
            return true;

        } catch (Exception e) {
            logger.error("证书验证过程异常，设备ID: {}, 错误: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 初始化安全引擎
     */
    private void initializeSecurityEngine() {
        try {
            // 初始化国密算法库
            logger.info("初始化HiSec安全引擎");

            // 这里应该加载实际的国密算法库
            // 例如：System.loadLibrary("sm4_native");

            // 验证SM4算法可用性
            KeyGenerator keyGen = KeyGenerator.getInstance("AES"); // 模拟SM4
            keyGen.init(128);

            logger.info("HiSec安全引擎初始化完成");

        } catch (Exception e) {
            logger.error("HiSec安全引擎初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("安全引擎初始化失败", e);
        }
    }

    /**
     * 读取证书数据
     */
    private byte[] readCertificateData(InputStream certStream) {
        try {
            return certStream.readAllBytes();
        } catch (Exception e) {
            logger.error("读取证书数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算证书指纹
     */
    private String calculateCertFingerprint(byte[] certData) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(certData);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("计算证书指纹失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用SM4验证证书签名
     */
    private boolean verifyCertSignatureWithSM4(byte[] certData, String deviceId) {
        try {
            // 模拟SM4签名验证过程
            logger.debug("使用SM4算法验证证书签名");

            // 1. 提取证书中的签名数据（模拟）
            byte[] signature = extractSignatureFromCert(certData);

            // 2. 获取用于验证的SM4密钥
            SecretKey sm4Key = getSM4VerificationKey(deviceId);

            // 3. 使用SM4解密签名
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // 模拟SM4
            cipher.init(Cipher.DECRYPT_MODE, sm4Key);
            byte[] decryptedSignature = cipher.doFinal(signature);

            // 4. 验证解密后的签名
            return verifyDecryptedSignature(decryptedSignature, certData);

        } catch (Exception e) {
            logger.error("SM4签名验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证证书链
     */
    private boolean verifyCertificateChain(byte[] certData) {
        try {
            logger.debug("验证证书链");

            // 模拟证书链验证
            // 实际实现应该：
            // 1. 解析证书链
            // 2. 验证每级证书的签名
            // 3. 检查证书有效期
            // 4. 验证到根证书

            // 这里简化处理，实际应该实现完整的证书链验证
            return certData.length > 100; // 简单的长度检查

        } catch (Exception e) {
            logger.error("证书链验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证设备ID匹配性
     */
    private boolean verifyDeviceIdMatch(String deviceId, byte[] certData) {
        try {
            // 从证书中提取设备标识信息
            String certDeviceId = extractDeviceIdFromCert(certData);

            // 比较设备ID
            boolean matched = deviceId.equals(certDeviceId);
            logger.debug("设备ID匹配检查: {} vs {}, 结果: {}", deviceId, certDeviceId, matched);

            return matched;

        } catch (Exception e) {
            logger.error("设备ID匹配验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查证书撤销状态
     */
    private boolean checkCertificateRevocationStatus(String certFingerprint) {
        try {
            // 模拟CRL检查
            // 实际实现应该：
            // 1. 查询CRL列表
            // 2. 检查OCSP状态
            // 3. 验证证书是否被撤销

            logger.debug("检查证书撤销状态: {}", certFingerprint);

            // 简化实现：假设证书未被撤销
            return true;

        } catch (Exception e) {
            logger.error("证书撤销状态检查失败: {}", e.getMessage());
            return false;
        }
    }

    // 辅助方法

    private byte[] extractSignatureFromCert(byte[] certData) {
        // 模拟从证书中提取签名
        // 实际应该解析ASN.1格式的证书
        int signatureLength = Math.min(128, certData.length / 4);
        byte[] signature = new byte[signatureLength];
        System.arraycopy(certData, certData.length - signatureLength, signature, 0, signatureLength);
        return signature;
    }

    private SecretKey getSM4VerificationKey(String deviceId) {
        try {
            // 模拟生成SM4密钥
            // 实际应该从安全存储中获取预共享密钥
            byte[] keyBytes = (ROOT_CERT_PUBLIC_KEY + deviceId).getBytes();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedKey = md.digest(keyBytes);
            byte[] sm4Key = new byte[16]; // SM4使用128位密钥
            System.arraycopy(hashedKey, 0, sm4Key, 0, 16);

            return new SecretKeySpec(sm4Key, "AES"); // 模拟SM4

        } catch (Exception e) {
            logger.error("生成SM4密钥失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean verifyDecryptedSignature(byte[] decryptedSignature, byte[] certData) {
        // 模拟签名验证
        // 实际应该计算证书内容的哈希值并与解密的签名进行比较
        return decryptedSignature.length > 0 && certData.length > 0;
    }

    private String extractDeviceIdFromCert(byte[] certData) {
        // 模拟从证书中提取设备ID
        // 实际应该解析证书的Subject或SAN字段

        // 简化实现：使用证书哈希的一部分作为设备ID
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(certData);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // 取前8字节
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown_device";
        }
    }
}