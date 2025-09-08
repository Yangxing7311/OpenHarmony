package org.example.service;

// ==================== 状态监听 - MQTT订阅 ====================

import java.util.List;

/**
 * MQTT设备状态监听服务
 */
@Service
public class DeviceStatusListener {

    private final MqttAsyncClient mqttClient;
    private final DeviceStatusHandler statusHandler;

    @PostConstruct
    public void initializeMQTT() {
        try {
            String broker = "ssl://iot-mqtts.cn-north-4.myhuaweicloud.com:8883";
            String clientId = "OpenHarmonyPlatform_" + System.currentTimeMillis();

            mqttClient = new MqttAsyncClient(broker, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(System.getenv("MQTT_USERNAME"));
            options.setPassword(System.getenv("MQTT_PASSWORD").toCharArray());
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);

            mqttClient.connect(options).waitForCompletion();
            log.info("MQTT客户端连接成功");

        } catch (Exception e) {
            log.error("MQTT初始化失败", e);
            throw new RuntimeException("MQTT连接失败", e);
        }
    }

    /**
     * 订阅设备状态主题
     */
    public void subscribeDeviceStatus(String deviceId) {
        try {
            String topic = "$hw/events/device/status/" + deviceId;

            mqttClient.subscribe(topic, 1, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    handleStatusMessage(deviceId, message);
                }
            });

            log.info("订阅设备状态成功：deviceId={}, topic={}", deviceId, topic);

        } catch (Exception e) {
            log.error("订阅设备状态失败：deviceId={}", deviceId, e);
        }
    }

    /**
     * 批量订阅设备状态
     */
    public void subscribeMultipleDevices(List<String> deviceIds) {
        deviceIds.forEach(this::subscribeDeviceStatus);
    }

    /**
     * 处理状态消息
     */
    private void handleStatusMessage(String deviceId, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            DeviceStatusEvent event = objectMapper.readValue(payload, DeviceStatusEvent.class);

            log.info("收到设备状态变化：deviceId={}, status={}",
                    deviceId, event.getStatus());

            // 异步处理状态变化
            statusHandler.handleStatusChange(deviceId, event);

        } catch (Exception e) {
            log.error("处理设备状态消息失败：deviceId={}", deviceId, e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.error("MQTT清理失败", e);
        }
    }
}
