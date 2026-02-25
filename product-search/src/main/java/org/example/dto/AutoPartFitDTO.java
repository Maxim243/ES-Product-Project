package org.example.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.example.enums.FitStatus;

@Data
@Builder
@Jacksonized
public class AutoPartFitDTO {

    private String sku;
    private Integer partGroupId;
    private String partName;
    private Integer brandId;
    private Integer modelId;
    private Integer yearId;
    private Integer engineId;
    private Integer bodyTypeId;
    private Integer transmissionId;
    private Integer fuelTypeId;
    private Integer hoodId;
    private Integer brakeSystemId;
    private Integer drivetrainId;
    private Integer suspensionId;
    private Integer wheelDriveId;
    private Integer steeringId;
    private Integer absId;
    private Integer airbagId;
    private Integer exhaustId;
    private Integer coolingId;
    private Integer lightingId;
    private FitStatus fitStatus;
}
