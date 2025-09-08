package org.example.template;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

/**
 * 设备命令参数模板
 */
@Validated
public class DeviceCommandTemplate {

    @NotBlank(message = "设备ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{8,32}$", message = "设备ID格式不正确")
    @JsonProperty("device_id")
    private String deviceId;

    @NotBlank(message = "命令类型不能为空")
    @JsonProperty("command_type")
    private String commandType;

    @Valid
    @JsonProperty("parameters")
    private CommandParameters parameters;

    @JsonProperty("region")
    private String region; // 华北/华东

    // getter/setter省略...


    public String getCommandType() {
        return commandType;
    }

    public CommandParameters getParameters() {
        return parameters;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRegion() {
        return region;
    }

    public static class CommandParameters {
        @DecimalMin(value = "0.0", message = "数值不能为负")
        @DecimalMax(value = "100.0", message = "数值不能超过100")
        private Double value;

        @Pattern(regexp = "^(on|off|auto)$", message = "状态值只能是on/off/auto")
        private String status;

        // getter/setter省略...
    }
}
