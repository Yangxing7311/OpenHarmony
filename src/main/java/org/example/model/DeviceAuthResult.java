package org.example.model;

/**
 * 设备认证结果
 */
public class DeviceAuthResult {
    private String deviceId;
    private boolean success;
    private String message;
    private long timestamp;
    private DeviceCertificate certificate;
    private String authMethod;
    private String errorCode;

    public DeviceAuthResult() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 检查认证结果是否过期
     */
    public boolean isExpired() {
        long expireTime = 3600000; // 1小时
        return (System.currentTimeMillis() - timestamp) > expireTime;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public DeviceCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(DeviceCertificate certificate) {
        this.certificate = certificate;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "DeviceAuthResult{" +
                "deviceId='" + deviceId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", authMethod='" + authMethod + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}