package org.example.service;

/**
 * 参数校验服务
 */
@Service
public class ParameterValidationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 验证JSON参数模板
     */
    public ValidationResult validateCommandTemplate(String jsonParams) {
        try {
            // 1. JSON格式校验
            DeviceCommandTemplate template = objectMapper.readValue(jsonParams, DeviceCommandTemplate.class);

            // 2. 注解校验
            Set<ConstraintViolation<DeviceCommandTemplate>> violations = validator.validate(template);

            if (!violations.isEmpty()) {
                List<String> errors = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.toList());
                return ValidationResult.failure(errors);
            }

            // 3. 业务规则校验
            if (!isValidRegion(template.getRegion())) {
                return ValidationResult.failure("不支持的地域：" + template.getRegion());
            }

            return ValidationResult.success(template);

        } catch (JsonProcessingException e) {
            return ValidationResult.failure("JSON格式错误：" + e.getMessage());
        }
    }

    private boolean isValidRegion(String region) {
        return Arrays.asList("华北", "华东", "NORTH_CHINA", "EAST_CHINA").contains(region);
    }
}
