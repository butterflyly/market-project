package com.example.demo.Weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${weather.api.service-key}")
    private String serviceKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 격자 좌표(nx, ny)로 단기예보(기온, 습도, 강수확률) 조회
     */
    public Map<String, String> getWeatherInfo(int nx, int ny) {
        String[] baseDateTime = getLatestBaseDateTime();
        String baseDate = baseDateTime[0];
        String baseTime = baseDateTime[1];

        // 1. URI 생성 (Decoding 키 기준 이중 인코딩 방지)
        String urlString = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "1000")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUriString();

        URI uri = URI.create(urlString);

        Map<String, String> resultMap = new HashMap<>();

        try {
            // 2. API 호출
            String jsonResponse = restTemplate.getForObject(uri, String.class);

            // 3. JSON 트리 파싱 (response -> body -> items -> item)
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String category = item.path("category").asText();
                    String fcstValue = item.path("fcstValue").asText();

                    // 필요한 항목만 뽑기 (TMP: 기온, REH: 습도, POP: 강수확률)
                    if ("TMP".equals(category) && !resultMap.containsKey("TMP")) {
                        resultMap.put("TMP", fcstValue + "℃");
                    } else if ("REH".equals(category) && !resultMap.containsKey("REH")) {
                        resultMap.put("REH", fcstValue + "%");
                    } else if ("POP".equals(category) && !resultMap.containsKey("POP")) {
                        resultMap.put("POP", fcstValue + "%");
                    }

                    if (resultMap.size() == 3) break; // 필요한 데이터 모두 수집 시 종료
                }
            }
        } catch (Exception e) {
            log.error("날씨 API 호출 중 예외 발생: {}", e.getMessage());
            resultMap.put("TMP", "-");
            resultMap.put("REH", "-");
            resultMap.put("POP", "-");
        }

        return resultMap;
    }

    /**
     * 기상청 단기예보 발표 시각(02, 05, 08, 11, 14, 17, 20, 23시) 기준 계산 유틸
     */
    private String[] getLatestBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();

        // 기상청 데이터는 발표 후 약 10분 뒤 제공되므로 10분 차감
        LocalDateTime target = now.minusMinutes(10);

        int hour = target.getHour();
        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};

        int selectedHour = 23;
        if (hour < 2) {
            target = target.minusDays(1); // 새벽 2시 전이면 전날 23시 예보 사용
        } else {
            for (int i = baseHours.length - 1; i >= 0; i--) {
                if (hour >= baseHours[i]) {
                    selectedHour = baseHours[i];
                    break;
                }
            }
        }

        String baseDate = target.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = String.format("%02d00", selectedHour);

        return new String[]{baseDate, baseTime};
    }
}