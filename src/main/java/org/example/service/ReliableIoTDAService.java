package org.example.service;

/**
 * 带重试的IoTDA服务
 */
@Service
public class ReliableIoTDAService {

    private final IoTDAService iotdaService;
    private final RetryTemplate retryTemplate;

    /**
     * 带重试的命令发送
     */
    public CommandResponse retryCommand(DeviceCommandTemplate command) {
        return retryTemplate.execute(context -> {
            log.info("尝试发送命令，第{}次重试：deviceId={}",
                    context.getRetryCount() + 1, command.getDeviceId());

            try {
                return iotdaService.sendDeviceCommand(command);
            } catch (Exception e) {
                log.warn("命令发送失败，准备重试：deviceId={}, error={}",
                        command.getDeviceId(), e.getMessage());
                throw e;
            }
        }, context -> {
            // 重试失败回调
            log.error("命令发送最终失败：deviceId={}, 重试次数={}",
                    command.getDeviceId(), context.getRetryCount());
            return CommandResponse.failure(command.getDeviceId(), "重试失败");
        });
    }

    /**
     * 带重试的状态查询
     */
    public DeviceStatus retryQueryStatus(String deviceId, String region) {
        return retryTemplate.execute(context -> {
            return iotdaService.queryDeviceStatus(deviceId, region);
        });
    }

    /**
     * 批量重试命令
     */
    public List<CommandResponse> retryBatchCommands(List<DeviceCommandTemplate> commands) {
        return commands.parallelStream()
                .map(this::retryCommand)
                .collect(Collectors.toList());
    }
}
