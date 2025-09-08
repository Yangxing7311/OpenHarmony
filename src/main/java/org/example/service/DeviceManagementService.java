package org.example.service;
// ==================== 统一服务入口 ====================

import org.example.template.DeviceCommandTemplate;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备管控平台核心服务
 */
@Service
@Slf4j
public class DeviceManagementService {

    private final ParameterValidationService validationService;
    private final ReliableIoTDAService reliableIoTDAService;
    private final DeviceStatusListener statusListener;

    /**
     * 执行设备命令（完整流程）
     */
    public CommandResponse executeDeviceCommand(String jsonCommand) {
        // 1. 参数校验
        ValidationResult validation = validationService.validateCommandTemplate(jsonCommand);
        if (!validation.isSuccess()) {
            throw new InvalidParameterException("参数校验失败：" + validation.getErrors());
        }

        DeviceCommandTemplate command = validation.getData();

        // 2. 订阅设备状态（如果未订阅）
        statusListener.subscribeDeviceStatus(command.getDeviceId());

        // 3. 发送命令（带重试）
        return reliableIoTDAService.retryCommand(command);
    }

    /**
     * 批量设备管控
     */
    public BatchCommandResponse batchManageDevices(List<String> jsonCommands) {
        List<DeviceCommandTemplate> validCommands = jsonCommands.stream()
                .map(json -> {
                    ValidationResult validation = validationService.validateCommandTemplate(json);
                    if (!validation.isSuccess()) {
                        log.warn("命令参数校验失败：{}", validation.getErrors());
                        return null;
                    }
                    return validation.getData();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 订阅所有设备状态
        List<String> deviceIds = validCommands.stream()
                .map(DeviceCommandTemplate::getDeviceId)
                .collect(Collectors.toList());
        statusListener.subscribeMultipleDevices(deviceIds);

        // 批量执行命令
        List<CommandResponse> responses = reliableIoTDAService.retryBatchCommands(validCommands);

        return BatchCommandResponse.builder()
                .totalCount(jsonCommands.size())
                .successCount((int) responses.stream().filter(r -> r.isSuccess()).count())
                .responses(responses)
                .build();
    }
}