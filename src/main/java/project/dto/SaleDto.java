package project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class SaleDto {
    private Long id;

    private BigDecimal amountOfPayment;
    private BigDecimal amountOfAfterPayment;
    private String saleNumber;
    private String saleDate;

    private Long cashierId;
    private String cashierFirstName;
    private String cashierLastName;
    private String cashierMiddleName;

    private Long sellerId;
    private String sellerFirstName;
    private String sellerLastName;
    private String sellerMiddleName;

    private Long departmentId;
    private String departmentName;
    private String departmentAddress;

    private List<SaleItemDto> saleItems;
    private List<PaymentDto> payments;

    private Long contractorId;
    private String contractorName;
}
