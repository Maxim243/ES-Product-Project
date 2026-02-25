package org.example.mappers;

import co.elastic.clients.json.JsonData;
import org.example.dto.AutoPartDocDTO;
import org.example.dto.AutoPartFitDTO;
import org.example.enums.FitStatus;
import org.springframework.stereotype.Component;

@Component
public class AutoPartMapper {

    public AutoPartFitDTO mapJsonToAutoPartFitDTO(JsonData jsonData) {
        return jsonData.to(AutoPartFitDTO.class);
    }

    public AutoPartFitDTO mapAutoPartDocToAutoPartFitDTO(AutoPartDocDTO doc, FitStatus status) {
        return AutoPartFitDTO.builder()
                .sku(doc.sku())
                .partGroupId(doc.partGroupId())
                .partName(doc.partName())
                .brandId(doc.brandId())
                .modelId(doc.modelId())
                .yearId(doc.yearId())
                .engineId(doc.engineId())
                .bodyTypeId(doc.bodyTypeId())
                .transmissionId(doc.transmissionId())
                .fuelTypeId(doc.fuelTypeId())
                .hoodId(doc.hoodId())
                .brakeSystemId(doc.brakeSystemId())
                .drivetrainId(doc.drivetrainId())
                .suspensionId(doc.suspensionId())
                .wheelDriveId(doc.wheelDriveId())
                .steeringId(doc.steeringId())
                .absId(doc.absId())
                .airbagId(doc.airbagId())
                .exhaustId(doc.exhaustId())
                .coolingId(doc.coolingId())
                .lightingId(doc.lightingId())
                .fitStatus(status)
                .build();
    }

}
