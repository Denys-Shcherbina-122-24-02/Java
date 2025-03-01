package com.example.apartmentfinder;

import com.example.apartmentfinder.model.Apartment;
import com.example.apartmentfinder.model.ExchangeRate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/api")
public class ApartmentController {

    private static final String OLX_URL = "https://www.olx.ua/uk/nedvizhimost/kvartiry/prodazha-kvartir/dnepr/?currency=UAH";
    private static final String EXCHANGE_RATE_API = "https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5";

    @GetMapping("/apartments")
    public ResponseEntity<List<Apartment>> getApartments() throws IOException {
        Document doc = Jsoup.connect(OLX_URL).get();
        Elements elements = doc.select("div[data-cy=l-card]");
        List<Apartment> apartments = new ArrayList<>();

        for (Element element : elements) {
            String title = element.select(".css-1sq4ur2").text();
            String price = element.select(".css-6j1qjp").text();
            String link = "https://www.olx.ua" + element.select(".css-qo0cxu").attr("href");
            apartments.add(new Apartment(title, price, link));
        }
        return ResponseEntity.ok(apartments);
    }

    @GetMapping("/exchange-rate")
    public ResponseEntity<Double> getExchangeRate() {
        RestTemplate restTemplate = new RestTemplate();
        ExchangeRate[] rates = restTemplate.getForObject(EXCHANGE_RATE_API, ExchangeRate[].class);
        if (rates != null) {
            for (ExchangeRate rate : rates) {
                if ("USD".equals(rate.getCcy())) {
                    return ResponseEntity.ok(rate.getBuy());
                }
            }
        }
        return ResponseEntity.ok(0.0);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportToExcel() throws IOException {
        List<Apartment> apartments = getApartments().getBody();
        if (apartments == null) return ResponseEntity.badRequest().body("No data available");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Apartments");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Title");
        headerRow.createCell(1).setCellValue("Price");
        headerRow.createCell(2).setCellValue("Link");

        int rowNum = 1;
        for (Apartment apartment : apartments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(apartment.getTitle());
            row.createCell(1).setCellValue(apartment.getPrice());
            row.createCell(2).setCellValue(apartment.getLink());
        }

        try (FileOutputStream fileOut = new FileOutputStream("apartments.xlsx")) {
            workbook.write(fileOut);
        }
        workbook.close();
        return ResponseEntity.ok("Exported to apartments.xlsx");
    }
}

@Controller
@RequestMapping("/")
class WebController {
    private final ApartmentController apartmentController;

    public WebController(ApartmentController apartmentController) {
        this.apartmentController = apartmentController;
    }

    @GetMapping
    public String index(Model model) throws IOException {
        model.addAttribute("apartments", apartmentController.getApartments().getBody());
        model.addAttribute("exchangeRate", apartmentController.getExchangeRate().getBody());
        return "index";
    }
}