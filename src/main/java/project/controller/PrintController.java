package project.controller;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.PrintModeStyle;
import com.github.anastaciocintra.escpos.image.BitImageWrapper;
import com.github.anastaciocintra.escpos.image.BitonalThreshold;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.dto.Data;
import project.dto.PaymentDto;
import project.dto.SaleDto;
import project.dto.SaleItemDto;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.github.anastaciocintra.escpos.EscPos.CharacterCodeTable.CP866_Cyrillic_2;
import static com.github.anastaciocintra.escpos.EscPos.CutMode.FULL;
import static com.github.anastaciocintra.escpos.EscPosConst.Justification.Center;

@RestController
@RequiredArgsConstructor
@RequestMapping()
public class PrintController {
    @GetMapping
    public String check() {
        return "Hello";
    }

    @PostMapping("/print")
    public ResponseEntity<?> print(@RequestBody Data data) {
        printDate(data);
        return new ResponseEntity<>(HttpStatus.OK, HttpStatus.valueOf(200));
    }

    public static void printDate(Data data) {
        SaleDto banner = data.getBanner();
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("\t\t").append(banner.getDepartmentName()).append("\n\n");
            sb.append("Sotuv: ").append(banner.getSaleNumber()).append("\n");
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
                } else sb.append(String.format("%-2d %-25s %-25s\n", number, itemName, priceDetails));
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
            String printerName;
            if (data.getPrinterName() == null || data.getPrinterName().isEmpty()) printerName = "XP-58 (copy 1)";
            else printerName = data.getPrinterName(); // 🔴 printer nomi
            printReceipt(printerName, data.getIpAddress(), data.getPort(), sb.toString(), banner);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.fillInStackTrace();
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
            if (!currentLine.isEmpty()) currentLine.append(" ");
            currentLine.append(word);
        }
        lines.add(currentLine.toString());

        return lines.toArray(new String[0]);
    }

    public static void printReceipt(String printerName, String ipAddress, int port, String data, SaleDto banner) {
        data = data.replaceAll("‘", "'");
        if (printerName != null && !printerName.isEmpty()) {
            try {
                PrintModeStyle b = new PrintModeStyle();
                b.setBold(true);

                PrintService printer = PrinterOutputStream.getPrintServiceByName(printerName);
                if (printer == null) throw new RuntimeException("Printer topilmadi!");
                PrinterOutputStream outputStream = new PrinterOutputStream(printer);
                EscPos escpos = new EscPos(outputStream);

                BitImageWrapper imageWrapper = new BitImageWrapper();
                imageWrapper.setJustification(Center);
                escpos.write(imageWrapper, new EscPosImage(new CoffeeImageImpl(ImageIO.read(new File("logo.png"))), new BitonalThreshold(127)));

                //ToDo: QRCode...
                /*QRCode qrcode = new QRCode();
                escpos.writeLF("QRCode size 6 and center justified");
                escpos.feed(2);
                qrcode.setSize(7);
                qrcode.setJustification(EscPosConst.Justification.Center);
                escpos.write(qrcode, "hello qrcode");
                escpos.feed(3);
                escpos.feed(5);*/

                escpos.writeLF("________________________________________________").writeLF("");
                escpos.write(b, "Sotuv: ").writeLF(banner.getSaleNumber());
                escpos.setCharacterCodeTable(CP866_Cyrillic_2);
                escpos.writeLF(b, "Qatortol 2 ko'chasi 22, 100096, Тоshkent, O'zbekiston");
                escpos.write(b, "Sana: ").writeLF(banner.getSaleDate());
//                escpos.writeLF(b, "Sotuvchi: ").write(banner.getSellerFirstName() + " " + banner.getSellerLastName() + " " + banner.getSellerMiddleName() + "\n");
                escpos.write(b, "Kassir: ").writeLF(banner.getCashierFirstName() + " " + banner.getCashierLastName() + " " + banner.getCashierMiddleName());
//                escpos.writeLF(b, "Mijoz: ").write(banner.getContractorName() + "\n");
                escpos.writeLF(b, "Kontaktlar: +99899 303 5555");
                escpos.writeLF(b, "            +99899 658 5555");
                escpos.writeLF(b, "INN: ");
                escpos.writeLF("________________________________________________").writeLF("");
                Integer number = 1;
                escpos.writeLF(b, String.format("%-2s %-25s %-25s", "№", "Mahsulot nomi", ""));
                for (SaleItemDto saleItem : banner.getSaleItems()) {
                    String itemName = saleItem.getItemSku() + " " + saleItem.getItemName();
                    BigDecimal totalPrice = saleItem.getCount().multiply(saleItem.getPriceRetailSale());
                    String priceDetails = saleItem.getCount() + " x " + saleItem.getPriceRetailSale() + " = " + totalPrice;

                    if (itemName.length() > 25) {
                        String[] wrappedName = wrapText(itemName, 25);
                        escpos.write(String.format("%-2s %-25s %-25s\n", number + ".", wrappedName[0], priceDetails));
                        for (int i = 1; i < wrappedName.length; i++)
                            escpos.write(String.format("%-2s %-25s %-25s\n", "", wrappedName[i], ""));
                    } else escpos.write(String.format("%-2s %-25s %-25s\n", number + ".", itemName, priceDetails));
                    number++;
                }
                escpos.writeLF("________________________________________________").writeLF("");
                escpos.writeLF(b, "To'lov turi: ");
                for (PaymentDto payment : banner.getPayments())
                    escpos.writeLF(String.format(" %-20s %s", payment.getPaymentTypeNameUz(), payment.getAmount() + " UZS"));
                escpos.writeLF("");
                BigDecimal payment = banner.getAmountOfPayment();
                BigDecimal afterPayment = banner.getAmountOfAfterPayment();
                if (payment.compareTo(afterPayment) > 0) {
                    escpos.writeLF(b, String.format("%-20s %-25s", "Jami:", payment + " UZS"));
                    BigDecimal discount = payment.subtract(afterPayment);
                    escpos.writeLF(b, String.format("%-20s %-25s", "Chegirma:", discount + " UZS"));
                } else escpos.writeLF(b, String.format("%-20s %-25s", "Jami:", afterPayment + " UZS"));
                escpos.writeLF("________________________________________________").writeLF("");
                escpos.write(new BitImageWrapper(), new EscPosImage(new CoffeeImageImpl(ImageIO.read(new File("instagram.jpg"))), new BitonalThreshold(127)))
                        .write("_italy_roma_");
                escpos.feed(1);
                escpos.write(new BitImageWrapper(), new EscPosImage(new CoffeeImageImpl(ImageIO.read(new File("telegram.jpg"))), new BitonalThreshold(127)))
                        .write("@_italy_roma_");
                escpos.feed(1);
                escpos.writeLF("________________________________________________");
                escpos.writeLF(b, "\n          Xaridingiz uchun rahmat!\n\n\n\n\n\n");
                escpos.cut(FULL);
                escpos.close();
            } catch (Exception e) {
                System.err.println("Error while printing: " + e.getMessage());
                e.fillInStackTrace();
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
                e.fillInStackTrace();
            }
        }
    }
}

