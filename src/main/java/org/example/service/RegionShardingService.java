package org.example.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地域分片服务
 */
@Service
public class RegionShardingService {

    private static final Map<String, List<String>> REGION_MAPPING = Map.of(
            "华北", Arrays.asList("beijing", "tianjin", "hebei", "shanxi", "neimenggu"),
            "华东", Arrays.asList("shanghai", "jiangsu", "zhejiang", "anhui", "fujian", "jiangxi", "shandong")
    );

    /**
     * 根据设备ID和地域进行分片
     */
    public ShardInfo getShardByRegion(String deviceId, String region) {
        String normalizedRegion = normalizeRegion(region);

        // 计算分片索引
        int shardIndex = Math.abs(deviceId.hashCode()) % getShardCount(normalizedRegion);

        return ShardInfo.builder()
                .region(normalizedRegion)
                .shardIndex(shardIndex)
                .endpoint(getRegionEndpoint(normalizedRegion))
                .build();
    }

    /**
     * 批量任务分片
     */
    public Map<String, List<DeviceTask>> shardTasksByRegion(List<DeviceTask> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(task -> {
                    ShardInfo shard = getShardByRegion(task.getDeviceId(), task.getRegion());
                    return shard.getRegion() + "_" + shard.getShardIndex();
                }));
    }

    private String normalizeRegion(String region) {
        if ("华北".equals(region) || "NORTH_CHINA".equals(region)) {
            return "华北";
        } else if ("华东".equals(region) || "EAST_CHINA".equals(region)) {
            return "华东";
        }
        throw new IllegalArgumentException("不支持的地域：" + region);
    }

    private int getShardCount(String region) {
        return "华北".equals(region) ? 3 : 4; // 华北3个分片，华东4个分片
    }

    private String getRegionEndpoint(String region) {
        return "华北".equals(region) ?
                "iotda.cn-north-4.myhuaweicloud.com" :
                "iotda.cn-east-3.myhuaweicloud.com";
    }
}
