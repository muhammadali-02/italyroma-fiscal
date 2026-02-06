package project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private BigDecimal amount;

    private Long paymentTypeId;
    private String paymentTypeNameUz;
    private String paymentTypeNameRu;
    private String paymentTypeNameEn;
}
