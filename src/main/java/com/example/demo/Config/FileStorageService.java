package com.example.demo.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-root}") // E:/Temp/
    private String rootPath;

    /**
     * 물리적 파일 저장
     * @param file 저장할 파일
     * @param subFolder 저장될 세부 폴더명 (예: itemImageUpload/)
     * @param urlPath 브라우저 접근 경로 (예: /itemImages/)
     * @return DB에 저장할 URL 완성본
     */
    public String save(MultipartFile file, String subFolder, String urlPath) {
        if (file == null || file.isEmpty()) return null;

        String fullUploadPath = null;
        try {

            //  물리적 저장 경로 설정
            fullUploadPath = rootPath + subFolder;
            File folder = new File(fullUploadPath);
            if (!folder.exists()) folder.mkdirs(); // 폴더 생성

            //  파일명 생성
            String originalFilename = file.getOriginalFilename();
            String storeFileName = UUID.randomUUID() + "_" + originalFilename;

            //  파일 저장
            file.transferTo(new File(fullUploadPath + storeFileName));

            //  DB 저장용 URL 리턴 (/itemImages/uuid_name.jpg)
            return urlPath + storeFileName;

        } catch (IOException e) {
            log.error("File storage error: filename={}, path={}", file.getOriginalFilename(), fullUploadPath, e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 물리적 파일 삭제
     */
    public void delete(String imageUrl, String subFolder, String urlPath) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        // 1. DB URL(/profileImages/abc.jpg)에서 파일명만 추출 -> "abc.jpg"
        String filename = imageUrl.replace(urlPath, "");

        try {
            // 2. Paths.get()을 사용하여 OS에 맞는 완벽한 절대 경로 생성
            // 예: "E:", "Temp", "profileImageUpload", "abc.jpg" 가 안전하게 결합됨
            Path filePath = Paths.get(rootPath, subFolder, filename);

            // 3. 파일이 존재하면 삭제 수행
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception e) {
            // 파일 삭제 중 에러가 나더라도 비즈니스 로직(회원수정) 전체가 롤백되지 않도록 로그만 남김
            log.info("파일 삭제 실패: " + imageUrl + " -> " + e.getMessage());
        }
    }

}
