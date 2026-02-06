package project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Data {
    private String printerName;
    private int port;
    private String ipAddress;
    private SaleDto banner;
}
