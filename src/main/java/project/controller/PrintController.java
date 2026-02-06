package project.controller;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.dto.Data;
import project.dto.PaymentDto;
import project.dto.SaleDto;
import project.dto.SaleItemDto;

import javax.print.PrintService;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.github.anastaciocintra.escpos.EscPos.CharacterCodeTable.CP866_Cyrillic_2;
import static com.github.anastaciocintra.escpos.EscPos.CutMode.FULL;

@RestController
@RequiredArgsConstructor
@RequestMapping()
public class PrintController {
    @GetMapping
    public String check() {
        return "Hello";
    }

    @PostMapping("/print")
    public ResponseEntity<?> print(
            @RequestBody Data data) {

        printDate(data);

        return new ResponseEntity<>(HttpStatus.OK, HttpStatus.valueOf(200));

    }

    public static void printDate(Data data) {
        SaleDto banner = data.getBanner();
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("\t\t").append(banner.getDepartmentName()).append("\n\n");
            sb.append("Chek nomeri: ").append(banner.getSaleNumber()).append("\n");
            if (banner.getContractorName() != null && !banner.getContractorName().isEmpty()) {

                sb.append("Mijoz: ").append(banner.getContractorName()).append("\n");

            }
            sb.append("Sana: ").append(banner.getSaleDate()).append("\n");
            sb.append("Kassir: ").append(banner.getCashierFirstName()).append(" ").append(banner.getCashierLastName()).append(" ").append(banner.getCashierMiddleName());
            sb.append("\n------------------------------------------------\n");

            int number = 1;
            sb.append(String.format("%-2s %-25s %-25s\n", "№", "Mahsulot nomi", ""));
            sb.append("\n");

            for (SaleItemDto saleItem : banner.getSaleItems()) {
                String itemName = saleItem.getItemSku() + " " + saleItem.getItemName();
                BigDecimal totalPrice = saleItem.getCount().multiply(saleItem.getPriceRetailSale());
                String priceDetails = saleItem.getCount() + " x " + saleItem.getPriceRetailSale() + " = " + totalPrice;

                if (itemName.length() > 25) {
                    String[] wrappedName = wrapText(itemName, 25);
                    sb.append(String.format("%-2d %-25s %-25s\n", number, wrappedName[0], priceDetails));
                    for (int i = 1; i < wrappedName.length; i++) {
                        sb.append(String.format("%-2s %-25s %-25s\n", "", wrappedName[i], ""));
                    }
                } else {
                    sb.append(String.format("%-2d %-25s %-25s\n", number, itemName, priceDetails));
                }
                number++;
            }

            sb.append("------------------------------------------------\n\n");
            sb.append("To'lov turi:\n");
            for (PaymentDto payment : banner.getPayments()) {
                sb.append(String.format(" %-20s %s\n", payment.getPaymentTypeNameUz(), payment.getAmount() + " UZS\n"));
            }

            BigDecimal payment = banner.getAmountOfPayment();
            BigDecimal afterPayment = banner.getAmountOfAfterPayment();

            if (payment.compareTo(afterPayment) > 0) {
                sb.append(String.format("%-20s %-25s\n", "Jami:", payment + " UZS"));
                BigDecimal discount = payment.subtract(afterPayment);
                sb.append(String.format("%-20s %-25s\n\n", "Chegirma:", discount + " UZS"));

            } else {
                sb.append(String.format("%-20s %-25s\n\n", "Jami:", afterPayment + " UZS"));
            }

            sb.append("\n\n\tXaridingiz uchun rahmat!\n\n");
            String printerName = "XP-58"; // 🔴 printer nomi

            printReceipt(printerName, data.getIpAddress(), data.getPort(), sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String[] wrapText(String text, int width) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > width) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        lines.add(currentLine.toString());

        return lines.toArray(new String[0]);
    }

    public static void printReceipt(String printerName, String ipAddress, int port, String data) {
        data = data.replaceAll("‘", "'");
        if (printerName != null && !printerName.isEmpty()) {
            try {
                PrintService printer = PrinterOutputStream.getPrintServiceByName(printerName);
                if (printer == null) throw new RuntimeException("Printer topilmadi!");
                PrinterOutputStream outputStream = new PrinterOutputStream(printer);
                EscPos escpos = new EscPos(outputStream);
                escpos
                        .setCharacterCodeTable(CP866_Cyrillic_2)
                        .write(data)
                        .cut(FULL);

                escpos.close();
            } catch (Exception e) {
                System.err.println("Error while printing: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try (Socket socket = new Socket(ipAddress, port)) {
                OutputStream outputStream = socket.getOutputStream();
                byte[] setEncodingCommand = new byte[]{0x1B, 0x74, 0x11};
                outputStream.write(setEncodingCommand);

                byte[] receiptData = data.getBytes(Charset.forName("CP866"));
                outputStream.write(receiptData);

                outputStream.write("\n\n\n\n".getBytes(Charset.forName("CP866")));

                byte[] cutPaperCommand = new byte[]{0x1D, 0x56, 0x00};
                outputStream.write(cutPaperCommand);

                outputStream.flush();

            } catch (Exception e) {
                System.err.println("Error while printing: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

