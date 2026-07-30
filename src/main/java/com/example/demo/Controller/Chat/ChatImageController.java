package com.example.demo.Controller.Chat;

import com.example.demo.Config.FileStorageService;
import com.example.demo.DTO.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatImageController {

    private final FileStorageService fileStorageService;

    private static final String CHAT_SUB_FOLDER = "chatImageUpload/";
    private static final String CHAT_URL_PREFIX = "/chatImages/";

    @PostMapping("/image")
    public ResponseEntity<CommonResponse<String>> uploadChatImage(
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(new CommonResponse<>(false, "업로드할 이미지가 없습니다.", null));
        }

        //  정해진 상수를 활용해 파일 저장소에 안전하게 저장
        String imageUrl = fileStorageService.save(image, CHAT_SUB_FOLDER, CHAT_URL_PREFIX);

        return ResponseEntity.ok(new CommonResponse<>(true, "이미지 업로드 성공", imageUrl));
    }
}