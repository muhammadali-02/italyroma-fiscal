
package project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SaleItemDto {
    private Long id;
    private BigDecimal priceRetailSale;
    private BigDecimal afterPrice;
    private BigDecimal count;

    private Long itemId;
    private String itemName;
    private String itemSku;

    private BigDecimal qty;
    private Long priceTypeId;
    private String priceTypeName;

    private Long discountId;
    private Long discountItemId;
    private BigDecimal oldPriceRetailSaleForOneTimeDiscount;

}
