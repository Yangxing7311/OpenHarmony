package org.example.service;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.huaweicloud.sdk.iotda.v5.region.IoTDARegion;
import org.example.template.DeviceCommandTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IoTDA服务客户端
 */
@Service
public class IoTDAService {

    private final IoTDAClient iotdaClient;
    private final RegionShardingService shardingService;

    public IoTDAService(RegionShardingService shardingService) {
        this.shardingService = shardingService;
        this.iotdaClient = initializeClient();
    }

    /**
     * 发送设备命令
     */
    public CommandResponse sendDeviceCommand(DeviceCommandTemplate command) {
        try {
            // 1. 获取分片信息
            ShardInfo shard = shardingService.getShardByRegion(
                    command.getDeviceId(),
                    command.getRegion()
            );

            // 2. 构建华为云命令请求
            CreateCommandRequest request = new CreateCommandRequest()
                    .withDeviceId(command.getDeviceId())
                    .withBody(buildCommandBody(command));

            // 3. 调用华为云API
            CreateCommandResponse response = iotdaClient.createCommand(request);

            return CommandResponse.builder()
                    .commandId(response.getCommandId())
                    .status("PENDING")
                    .region(shard.getRegion())
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("发送设备命令失败：deviceId={}, error={}",
                    command.getDeviceId(), e.getMessage());
            throw new IoTDAException("命令发送失败", e);
        }
    }

    /**
     * 批量发送命令
     */
    public List<CommandResponse> batchSendCommands(List<DeviceCommandTemplate> commands) {
        // 按地域分片
        Map<String, List<DeviceCommandTemplate>> shardedCommands =
                commands.stream().collect(Collectors.groupingBy(cmd ->
                        shardingService.getShardByRegion(cmd.getDeviceId(), cmd.getRegion()).getRegion()
                ));

        // 并行处理各分片
        return shardedCommands.entrySet().parallelStream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(this::sendDeviceCommand))
                .collect(Collectors.toList());
    }

    /**
     * 查询设备状态
     */
    public DeviceStatus queryDeviceStatus(String deviceId, String region) {
        ShardInfo shard = shardingService.getShardByRegion(deviceId, region);

        ShowDeviceRequest request = new ShowDeviceRequest().withDeviceId(deviceId);
        ShowDeviceResponse response = iotdaClient.showDevice(request);

        return DeviceStatus.builder()
                .deviceId(deviceId)
                .status(response.getStatus())
                .lastActiveTime(response.getActiveTime())
                .region(shard.getRegion())
                .build();
    }

    private IoTDAClient initializeClient() {
        BasicCredentials auth = new BasicCredentials()
                .withAk(System.getenv("HUAWEI_AK"))
                .withSk(System.getenv("HUAWEI_SK"));

        return IoTDAClient.newBuilder()
                .withCredential(auth)
                .withRegion(IoTDARegion.CN_NORTH_4)
                .build();
    }

    private DeviceCommandRequest buildCommandBody(DeviceCommandTemplate template) {
        return new DeviceCommandRequest()
                .withServiceId("WaterMeter")
                .withCommandName(template.getCommandType())
                .withParas(template.getParameters());
    }
}