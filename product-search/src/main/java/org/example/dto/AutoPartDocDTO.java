package org.example.dto;

public record AutoPartDocDTO(
        String sku,
        Integer partGroupId,
        String partName,
        Integer brandId,
        Integer modelId,
        Integer yearId,
        Integer engineId,
        Integer bodyTypeId,
        Integer transmissionId,
        Integer fuelTypeId,
        Integer hoodId,
        Integer brakeSystemId,
        Integer drivetrainId,
        Integer suspensionId,
        Integer wheelDriveId,
        Integer steeringId,
        Integer absId,
        Integer airbagId,
        Integer exhaustId,
        Integer coolingId,
        Integer lightingId
) {}

