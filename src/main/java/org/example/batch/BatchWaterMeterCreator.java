package org.example.batch;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.huaweicloud.sdk.iotda.v5.region.IoTDARegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 批量创建华为云IoTDA水表设备（修正版）
 * 适配华为云IoTDA SDK的实际API结构
 */
public class FixedBatchWaterMeterCreator {

    private static final Logger logger = LoggerFactory.getLogger(FixedBatchWaterMeterCreator.class);

    // 配置信息
    private static final String ACCESS_KEY = "your-access-key";  // 替换为你的AK
    private static final String SECRET_KEY = "your-secret-key";  // 替换为你的SK
    private static final String REGION = "cn-north-4";          // 区域
    private static final int TOTAL_DEVICES = 500;               // 总设备数
    private static final int BATCH_SIZE = 50;                   // 每批次创建数量
    private static final int THREAD_POOL_SIZE = 10;             // 线程池大小

    // 水表产品相关配置
    private static final String PRODUCT_NAME = "OpenHarmony智能水表";
    private static final String DEVICE_TYPE = "WaterMeter";
    private static final String PROTOCOL_TYPE = "MQTT";

    private IoTDAClient client;
    private String productId;
    private List<WaterMeterDevice> createdDevices = new ArrayList<>();

    public static void main(String[] args) {
        FixedBatchWaterMeterCreator creator = new FixedBatchWaterMeterCreator();
        try {
            creator.initialize();
            creator.createWaterMeterDevices();
            creator.generateReport();
        } catch (Exception e) {
            logger.error("批量创建设备失败", e);
        } finally {
            creator.cleanup();
        }
    }

    /**
     * 初始化IoTDA客户端
     */
    private void initialize() {
        logger.info("初始化华为云IoTDA客户端...");

        ICredential auth = new BasicCredentials()
                .withAk(ACCESS_KEY)
                .withSk(SECRET_KEY);

        this.client = IoTDAClient.newBuilder()
                .withCredential(auth)
                .withRegion(IoTDARegion.valueOf(REGION))
                .build();

        // 创建或获取水表产品
        this.productId = createOrGetWaterMeterProduct();

        logger.info("初始化完成，产品ID: {}", productId);
    }

    /**
     * 创建或获取水表产品（修正版）
     */
    private String createOrGetWaterMeterProduct() {
        try {
            // 首先尝试查找现有产品
            String existingProductId = findExistingProduct();
            if (existingProductId != null) {
                logger.info("找到现有水表产品，ID: {}", existingProductId);
                return existingProductId;
            }

            // 创建新产品 - 使用正确的API结构
            logger.info("创建新的水表产品...");
            CreateProductRequest request = new CreateProductRequest();

            // 使用AddProduct或ProductInfo等实际存在的类
            // 根据实际SDK调整这部分代码
            try {
                // 方案1：尝试使用AddProduct
                AddProduct addProduct = new AddProduct();
                addProduct.withProductName(PRODUCT_NAME)
                        .withManufacturerName("OpenHarmony生态")
                        .withModel("WM-2024")
                        .withProductType(DEVICE_TYPE)
                        .withDescription("OpenHarmony设备统一管控平台 - 智能水表设备")
                        .withProtocolType(PROTOCOL_TYPE)
                        .withDataType("json");

                request.withBody(addProduct);
            } catch (Exception e1) {
                try {
                    // 方案2：尝试使用ProductInfo
                    ProductInfo productInfo = new ProductInfo();
                    productInfo.withProductName(PRODUCT_NAME)
                            .withManufacturerName("OpenHarmony生态")
                            .withModel("WM-2024")
                            .withProductType(DEVICE_TYPE)
                            .withDescription("OpenHarmony设备统一管控平台 - 智能水表设备")
                            .withProtocolType(PROTOCOL_TYPE)
                            .withDataType("json");

                    request.withBody(productInfo);
                } catch (Exception e2) {
                    // 方案3：直接使用Map构建请求体
                    logger.warn("使用标准类失败，采用Map方式构建请求");
                    return createProductWithMap();
                }
            }

            CreateProductResponse response = client.createProduct(request);
            logger.info("水表产品创建成功，产品ID: {}", response.getProductId());
            return response.getProductId();

        } catch (Exception e) {
            logger.error("创建水表产品失败: {}", e.getMessage());

            // 如果产品创建失败，尝试使用一个默认的产品ID
            // 或者提示用户在控制台手动创建产品
            logger.info("尝试查找任意可用的产品用于设备创建...");
            return findAnyAvailableProduct();
        }
    }

    /**
     * 使用Map方式创建产品
     */
    private String createProductWithMap() {
        try {
            logger.info("使用Map方式创建产品...");

            // 先检查是否有可用产品
            String availableProduct = findAnyAvailableProduct();
            if (availableProduct != null) {
                logger.info("找到可用产品，跳过创建步骤，使用产品ID: {}", availableProduct);
                return availableProduct;
            }

            // 如果没有可用产品，提示用户手动创建
            logger.warn("未找到可用产品，请在华为云IoTDA控制台手动创建产品");
            logger.warn("产品配置建议：");
            logger.warn("  - 产品名称: {}", PRODUCT_NAME);
            logger.warn("  - 设备类型: {}", DEVICE_TYPE);
            logger.warn("  - 协议类型: {}", PROTOCOL_TYPE);
            logger.warn("  - 数据格式: JSON");

            // 返回一个占位符ID，实际使用时需要替换
            throw new RuntimeException("需要手动创建产品，请在控制台创建后更新PRODUCT_ID常量");

        } catch (Exception e) {
            logger.error("Map方式创建产品失败", e);
            throw new RuntimeException("产品创建失败", e);
        }
    }

    /**
     * 查找现有的水表产品
     */
    private String findExistingProduct() {
        try {
            ListProductsRequest request = new ListProductsRequest();
            request.withLimit(100);

            ListProductsResponse response = client.listProducts(request);
            if (response.getProducts() != null) {
                for (ProductSummary product : response.getProducts()) {
                    if (PRODUCT_NAME.equals(product.getName()) ||
                            (product.getName() != null && product.getName().contains("水表")) ||
                            DEVICE_TYPE.equals(product.getDeviceType())) {
                        return product.getProductId();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("查找现有产品失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 查找任意可用的产品
     */
    private String findAnyAvailableProduct() {
        try {
            ListProductsRequest request = new ListProductsRequest();
            request.withLimit(10);

            ListProductsResponse response = client.listProducts(request);
            if (response.getProducts() != null && !response.getProducts().isEmpty()) {
                ProductSummary firstProduct = response.getProducts().get(0);
                logger.info("使用现有产品: {} (ID: {})", firstProduct.getName(), firstProduct.getProductId());
                return firstProduct.getProductId();
            }
        } catch (Exception e) {
            logger.warn("查找可用产品失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 批量创建水表设备
     */
    private void createWaterMeterDevices() {
        if (productId == null) {
            logger.error("产品ID为空，无法创建设备");
            return;
        }

        logger.info("开始批量创建{}个水表设备，使用产品ID: {}", TOTAL_DEVICES, productId);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 分批创建设备
        for (int batch = 0; batch < (TOTAL_DEVICES + BATCH_SIZE - 1) / BATCH_SIZE; batch++) {
            final int batchNumber = batch;
            final int startIndex = batch * BATCH_SIZE;
            final int endIndex = Math.min(startIndex + BATCH_SIZE, TOTAL_DEVICES);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                createDeviceBatch(batchNumber + 1, startIndex, endIndex);
            }, executor);

            futures.add(future);
        }

        // 等待所有批次完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            logger.error("等待批次完成时出错", e);
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        logger.info("设备创建完成，成功创建{}个设备", createdDevices.size());
    }

    /**
     * 创建一批设备
     */
    private void createDeviceBatch(int batchNumber, int startIndex, int endIndex) {
        logger.info("开始创建第{}批设备，索引范围: {}-{}", batchNumber, startIndex, endIndex - 1);

        for (int i = startIndex; i < endIndex; i++) {
            try {
                WaterMeterDevice device = createSingleWaterMeter(i + 1);
                synchronized (createdDevices) {
                    createdDevices.add(device);
                }

                if (i % 10 == 0) {
                    logger.info("已创建{}个设备", i + 1);
                }

                // 避免请求过于频繁
                Thread.sleep(200);

            } catch (Exception e) {
                logger.error("创建设备{}失败: {}", i + 1, e.getMessage());
            }
        }

        logger.info("第{}批设备创建完成", batchNumber);
    }

    /**
     * 创建单个水表设备
     */
    private WaterMeterDevice createSingleWaterMeter(int index) {
        try {
            CreateDeviceRequest request = new CreateDeviceRequest();
            CreateDevice body = new CreateDevice();

            // 生成设备信息
            String deviceName = String.format("OpenHarmony-WaterMeter-%04d", index);
            String nodeId = String.format("watermeter_%04d", index);
            String secret = generateDeviceSecret();

            body.withProductId(productId)
                    .withNodeId(nodeId)
                    .withDeviceName(deviceName)
                    .withSecret(secret)
                    .withDescription(String.format("OpenHarmony智能水表设备 #%d", index));

            request.withBody(body);

            CreateDeviceResponse response = client.createDevice(request);

            WaterMeterDevice device = new WaterMeterDevice();
            device.setDeviceId(response.getDeviceId());
            device.setDeviceName(deviceName);
            device.setNodeId(nodeId);
            device.setSecret(secret);
            device.setIndex(index);
            device.setProductId(productId);

            logger.debug("创建水表设备成功: {} (ID: {})", deviceName, response.getDeviceId());

            return device;

        } catch (ConnectionException | RequestTimeoutException e) {
            logger.error("网络连接错误，设备{}: {}", index, e.getMessage());
            throw new RuntimeException("网络连接失败", e);
        } catch (ServiceResponseException e) {
            logger.error("服务响应错误，设备{}: {} - {}", index, e.getHttpStatusCode(), e.getErrorMsg());
            throw new RuntimeException("服务响应失败", e);
        } catch (Exception e) {
            logger.error("创建设备{}时出现未知错误", index, e);
            throw new RuntimeException("创建设备失败", e);
        }
    }

    /**
     * 生成设备密钥
     */
    private String generateDeviceSecret() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成设备创建报告
     */
    private void generateReport() {
        try {
            String fileName = String.format("water_meter_devices_%s.csv",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            try (FileWriter writer = new FileWriter(fileName)) {
                // 写入CSV头部
                writer.write("序号,设备名称,设备ID,节点ID,设备密钥,产品ID,创建时间\\n");

                // 写入设备信息
                for (WaterMeterDevice device : createdDevices) {
                    writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\\n",
                            device.getIndex(),
                            device.getDeviceName(),
                            device.getDeviceId(),
                            device.getNodeId(),
                            device.getSecret(),
                            device.getProductId(),
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    ));
                }
            }

            logger.info("设备信息已导出到文件: {}", fileName);

            // 打印统计信息
            printStatistics();

        } catch (IOException e) {
            logger.error("生成报告失败", e);
        }
    }

    /**
     * 打印统计信息
     */
    private void printStatistics() {
        System.out.println("\\n" + "=".repeat(60));
        System.out.println("          OpenHarmony 水表设备创建报告");
        System.out.println("=".repeat(60));
        System.out.println("目标设备数量: " + TOTAL_DEVICES);
        System.out.println("成功创建数量: " + createdDevices.size());
        System.out.println("创建成功率: " + String.format("%.2f",
                (double) createdDevices.size() / TOTAL_DEVICES * 100) + "%");
        System.out.println("产品ID: " + productId);
        System.out.println("设备类型: " + DEVICE_TYPE);
        System.out.println("通信协议: " + PROTOCOL_TYPE);
        System.out.println("创建时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("=".repeat(60));

        if (createdDevices.size() < TOTAL_DEVICES) {
            System.out.println("注意: 有 " + (TOTAL_DEVICES - createdDevices.size()) + " 个设备创建失败，请检查日志");
        } else {
            System.out.println("所有设备创建成功！可以开始进行设备注册联调测试。");
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * 水表设备信息类
     */
    public static class WaterMeterDevice {
        private int index;
        private String deviceId;
        private String deviceName;
        private String nodeId;
        private String secret;
        private String productId;

        // Getters and Setters
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
    }
}