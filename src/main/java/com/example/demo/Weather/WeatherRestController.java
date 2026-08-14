package com.example.demo.Weather;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherRestController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getWeather(
            @RequestParam(defaultValue = "37.5665") double lat, // 기본값: 서울 위도
            @RequestParam(defaultValue = "126.9780") double lon) { // 기본값: 서울 경도

        // 1. 위경도 -> 기상청 nx, ny 격자 변환
        int[] grid = convertToGrid(lat, lon);

        // 2. 날씨 정보 조회 (TMP, REH, POP)
        Map<String, String> weatherData = weatherService.getWeatherInfo(grid[0], grid[1]);

        return ResponseEntity.ok(weatherData);
    }

    // 위경도 -> 기상청 격자 좌표(nx, ny) 변환 로직
    private int[] convertToGrid(double lat, double lon) {
        double RE = 6371.00877; double GRID = 5.0; double SLAT1 = 30.0; double SLAT2 = 60.0;
        double OLON = 126.0; double OLAT = 38.0; double XO = 43; double YO = 136;
        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD; double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD; double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[]{x, y};
    }
}