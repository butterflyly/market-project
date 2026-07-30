package com.example.demo.Controller.Users;

import com.example.demo.DTO.Chat.Response.ChatRoomSummaryResponse;
import com.example.demo.DTO.Users.Request.Auth.LoginRequest;
import com.example.demo.DTO.Users.Request.Auth.Register.UserRegisterRequest;
import com.example.demo.DTO.Users.Response.MyPageResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.MyPageReadService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserViewController {

    private final UserReadService userReadService;
    private final ChatRoomReadService chatRoomReadService;
    private final ItemReadService itemReadService;
    private final MyPageReadService myPageReadService;

    // 회원가입 화면
    @GetMapping("/register")
    public String RegisterView(Model model) {

        model.addAttribute("registerForm" , new UserRegisterRequest("","","",
                "","",null,""));
        return "users/register";
    }

    // 로그인 화면
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "message", required = false) String message, // 시큐리티가 보낸 메시지 받기
            Model model) {

        // 화면단(JSP/JS)에서 세션 만료 알림을 띄울 수 있도록 모델에 적재
        if (message != null) {
            model.addAttribute("message", message);
        }

        model.addAttribute("loginForm", new LoginRequest("", "", false));
        return "users/login";
    }

    // 마이페이지 화면
    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal UserDetails userDetails
                            ,@PageableDefault(size = 5) Pageable pageable) {

        MyPageResponse myPageData = myPageReadService.getMyPageData(userDetails.getUsername(), pageable);
        model.addAttribute("myPageData", myPageData);

        return "users/mypage";
    }

    // 내 판매글 전체보기 페이지 (HTML 준비중)
    @GetMapping("/items/my-list")
    public String getMyItemsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        Page<ItemSummaryResponse> items =
                itemReadService.getItemsByUsername(userDetails.getUsername(), pageable);

        model.addAttribute("items", items);

        return "users/my-items";
    }

    // 내 채팅 전체보기 페이지 (HTML 준비중)
    @GetMapping("/mypage/chats")
    public String getMyChatRooms(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "BUYER") String role,
            @RequestParam(defaultValue = "0") int page,Model model) {

        // 2. "더보기"나 "페이지 클릭" 시 API로 데이터만 전달 (10개씩)
        Page<ChatRoomSummaryResponse> chatList =
                chatRoomReadService.getChatRooms(userDetails.getUsername(), role, page, 10);

        model.addAttribute("chats",chatList);

        return "users/my-chats";
    }

    // 회원 정보 수정 페이지 이동
    @GetMapping("/update")
    public String updatePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        model.addAttribute("user", userReadService.getUserResponseDTO(userDetails.getUsername()));
        return "users/user_update";
    }

    @GetMapping("/update/password")
    public String passwordUpdate()
    {
        return "users/password_update";
    }

    // 회원 탈퇴 비밀번호 확인 페이지 이동
    @GetMapping("/withdraw/confirm")
    public String withdrawConfirmPage() {
        return "users/withdraw_confirm";
    }


    /*
     아이디 찾기
     */
    @GetMapping("/find-userId")
    public String findAccount(Model model) {

        model.addAttribute("sendConfirm", false);
        model.addAttribute("error", false);

        return "users/userId_find";
    }

    /*
      비밀번호 찾기
     */
    @GetMapping("/find-password")
    public String findPassword(Model model) {

        model.addAttribute("sendConfirm", false);
        model.addAttribute("error", false);

        return "users/password_find";
    }

}
