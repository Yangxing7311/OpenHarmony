
import org.example.model.DeviceAuthResult;
import org.example.service.AuthService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService测试类
 * 验证设备认证功能是否正常工作
 */
public class AuthServiceTest {

    public static void main(String[] args) {
        System.out.println("=== OpenHarmony设备认证服务测试 ===");

        AuthService authService = new AuthService();

        // 测试1：单个设备证书验证
        testSingleDeviceAuth(authService);

        // 测试2：批量设备证书验证
        testBatchDeviceAuth(authService);

        // 测试3：缓存功能测试
        testAuthCache(authService);

        // 测试4：异常情况测试
        testExceptionHandling(authService);

        // 测试5：性能测试
        performanceTest(authService);

        System.out.println("\n=== 测试完成 ===");
        System.out.println("建议：");
        System.out.println("1. 检查认证成功率是否符合预期");
        System.out.println("2. 验证缓存机制是否正常工作");
        System.out.println("3. 确认异常处理是否健壮");
        System.out.println("4. 评估性能是否满足业务需求");
    }

    /**
     * 测试单个设备认证
     */
    private static void testSingleDeviceAuth(AuthService authService) {
        System.out.println("\n--- 测试1：单个设备证书验证 ---");

        try {
            // 创建模拟证书
            InputStream mockCertStream = createMockCertificate("device001");

            // 执行认证
            DeviceAuthResult result = authService.verifyDeviceCertificate("device001", mockCertStream);

            // 输出结果
            System.out.println("认证结果: " + result);
            System.out.println("认证状态: " + (result.isSuccess() ? "成功 ✅" : "失败 ❌"));

        } catch (Exception e) {
            System.err.println("单个设备认证测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试批量设备认证
     */
    private static void testBatchDeviceAuth(AuthService authService) {
        System.out.println("\n--- 测试2：批量设备证书验证 ---");

        try {
            // 创建多个模拟设备证书
            ConcurrentHashMap<String, InputStream> deviceCerts = new ConcurrentHashMap<>();
            deviceCerts.put("device001", createMockCertificate("device001"));
            deviceCerts.put("device002", createMockCertificate("device002"));
            deviceCerts.put("device003", createMockCertificate("device003"));
            deviceCerts.put("device004", createMockCertificate("device004"));
            deviceCerts.put("device005", createMockCertificate("device005"));

            System.out.println("准备验证 " + deviceCerts.size() + " 个设备证书...");

            // 记录开始时间
            long startTime = System.currentTimeMillis();

            // 执行批量认证
            ConcurrentHashMap<String, DeviceAuthResult> results =
                    authService.batchVerifyDeviceCertificates(deviceCerts);

            // 记录结束时间
            long endTime = System.currentTimeMillis();

            // 统计结果
            long successCount = results.values().stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failedCount = results.size() - successCount;

            System.out.println("批量认证完成:");
            System.out.println("  - 总数: " + results.size());
            System.out.println("  - 成功: " + successCount + " ✅");
            System.out.println("  - 失败: " + failedCount + " ❌");
            System.out.println("  - 耗时: " + (endTime - startTime) + "ms");

            // 输出详细结果
            results.forEach((deviceId, result) -> {
                System.out.println("  " + deviceId + ": " +
                        (result.isSuccess() ? "成功" : "失败 - " + result.getMessage()));
            });

        } catch (Exception e) {
            System.err.println("批量设备认证测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试认证缓存功能
     */
    private static void testAuthCache(AuthService authService) {
        System.out.println("\n--- 测试3：认证缓存功能 ---");

        try {
            String deviceId = "cache_test_device";
            InputStream certStream1 = createMockCertificate(deviceId);

            // 第一次认证
            System.out.println("第一次认证（应该执行完整验证）:");
            long startTime1 = System.currentTimeMillis();
            DeviceAuthResult result1 = authService.verifyDeviceCertificate(deviceId, certStream1);
            long endTime1 = System.currentTimeMillis();
            System.out.println("  结果: " + (result1.isSuccess() ? "成功" : "失败"));
            System.out.println("  耗时: " + (endTime1 - startTime1) + "ms");

            // 第二次认证（应该使用缓存）
            System.out.println("\n第二次认证（应该使用缓存）:");
            InputStream certStream2 = createMockCertificate(deviceId);
            long startTime2 = System.currentTimeMillis();
            DeviceAuthResult result2 = authService.verifyDeviceCertificate(deviceId, certStream2);
            long endTime2 = System.currentTimeMillis();
            System.out.println("  结果: " + (result2.isSuccess() ? "成功" : "失败"));
            System.out.println("  耗时: " + (endTime2 - startTime2) + "ms");

            // 验证缓存效果
            if (endTime2 - startTime2 < endTime1 - startTime1) {
                System.out.println("  缓存生效 ✅ (第二次认证更快)");
            } else {
                System.out.println("  缓存可能未生效 ⚠️");
            }

            // 输出缓存统计
            System.out.println("\n" + authService.getAuthStatistics());

        } catch (Exception e) {
            System.err.println("缓存功能测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试异常情况处理
     */
    private static void testExceptionHandling(AuthService authService) {
        System.out.println("\n--- 测试4：异常情况处理 ---");

        // 测试4.1：空证书流
        System.out.println("测试4.1：空证书流");
        try {
            DeviceAuthResult result = authService.verifyDeviceCertificate("empty_device",
                    new ByteArrayInputStream(new byte[0]));
            System.out.println("  结果: " + (result.isSuccess() ? "意外成功" : "正确失败 ✅"));
            System.out.println("  消息: " + result.getMessage());
        } catch (Exception e) {
            System.out.println("  异常处理: " + e.getMessage());
        }

        // 测试4.2：无效证书数据
        System.out.println("\n测试4.2：无效证书数据");
        try {
            byte[] invalidCert = "invalid certificate data".getBytes();
            DeviceAuthResult result = authService.verifyDeviceCertificate("invalid_device",
                    new ByteArrayInputStream(invalidCert));
            System.out.println("  结果: " + (result.isSuccess() ? "意外成功" : "正确失败 ✅"));
            System.out.println("  消息: " + result.getMessage());
        } catch (Exception e) {
            System.out.println("  异常处理: " + e.getMessage());
        }

        // 测试4.3：null参数
        System.out.println("\n测试4.3：null参数处理");
        try {
            DeviceAuthResult result = authService.verifyDeviceCertificate(null, null);
            System.out.println("  结果: " + (result.isSuccess() ? "意外成功" : "正确失败 ✅"));
        } catch (Exception e) {
            System.out.println("  异常处理正确 ✅: " + e.getMessage());
        }

        // 测试4.4：缓存清理
        System.out.println("\n测试4.4：缓存清理功能");
        try {
            authService.cleanExpiredCache();
            System.out.println("  缓存清理执行成功 ✅");
            System.out.println("  " + authService.getAuthStatistics());
        } catch (Exception e) {
            System.err.println("  缓存清理异常: " + e.getMessage());
        }
    }

    /**
     * 创建模拟证书数据
     */
    private static InputStream createMockCertificate(String deviceId) {
        try {
            // 生成模拟的X.509证书数据
            StringBuilder certBuilder = new StringBuilder();
            certBuilder.append("-----BEGIN CERTIFICATE-----\n");

            // 模拟证书内容（Base64编码的DER格式）
            // 这里使用简化的模拟数据
            String mockCertData = generateMockCertData(deviceId);
            certBuilder.append(mockCertData);
            certBuilder.append("\n-----END CERTIFICATE-----");

            return new ByteArrayInputStream(certBuilder.toString().getBytes());

        } catch (Exception e) {
            System.err.println("创建模拟证书失败: " + e.getMessage());
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * 生成模拟证书数据
     */
    private static String generateMockCertData(String deviceId) {
        try {
            // 使用Java内置的证书生成功能创建自签名证书
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            // 创建证书构建器（简化版）
            // 实际应该使用BouncyCastle等库来创建标准X.509证书

            // 这里返回一个简化的Base64编码数据
            String deviceInfo = "CN=" + deviceId + ",O=OpenHarmony,C=CN";
            String mockData = deviceInfo + keyPair.getPublic().toString();

            // 简单的Base64编码
            return java.util.Base64.getEncoder().encodeToString(mockData.getBytes());

        } catch (Exception e) {
            // 如果生成失败，返回一个固定的模拟数据
            String fixedMockData = "MIIDXTCCAkWgAwIBAgIJAKoK/heBjcOuMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV" +
                    "BAYTAkNOMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX" +
                    "aWRnaXRzIFB0eSBMdGQwHhcNMTYwODI0MTQwNjMwWhcNMTcwODI0MTQwNjMwWjBF" +
                    deviceId; // 添加设备ID使每个证书不同
            return fixedMockData;
        }
    }

    /**
     * 性能测试
     */
    private static void performanceTest(AuthService authService) {
        System.out.println("\n--- 性能测试 ---");

        try {
            int testCount = 100;
            System.out.println("开始性能测试，测试次数: " + testCount);

            long totalTime = 0;
            int successCount = 0;

            for (int i = 0; i < testCount; i++) {
                String deviceId = "perf_test_device_" + i;
                InputStream certStream = createMockCertificate(deviceId);

                long startTime = System.currentTimeMillis();
                DeviceAuthResult result = authService.verifyDeviceCertificate(deviceId, certStream);
                long endTime = System.currentTimeMillis();

                totalTime += (endTime - startTime);
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            double avgTime = (double) totalTime / testCount;
            double successRate = (double) successCount / testCount * 100;

            System.out.println("性能测试结果:");
            System.out.println("  - 总测试次数: " + testCount);
            System.out.println("  - 成功次数: " + successCount);
            System.out.println("  - 成功率: " + String.format("%.2f", successRate) + "%");
            System.out.println("  - 平均耗时: " + String.format("%.2f", avgTime) + "ms");
            System.out.println("  - 总耗时: " + totalTime + "ms");

            if (avgTime < 100) {
                System.out.println("  - 性能: 优秀 ✅");
            } else if (avgTime < 500) {
                System.out.println("  - 性能: 良好 ✅");
            } else {
                System.out.println("  - 性能: 需要优化 ⚠️");
            }

        } catch (Exception e) {
            System.err.println("性能测试异常: " + e.getMessage());
        }
    }
}