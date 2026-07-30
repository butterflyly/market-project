// 1. 페이지 로드 시 URL의 redirectUrl을 쿠키에 즉시 저장
document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const redirectUrl = urlParams.get('redirectUrl');

    // redirectUrl이 존재하고 로그인 페이지 자체가 아니면 5분짜리 쿠키 생성
    if (redirectUrl && !redirectUrl.includes('/users/login')) {
        document.cookie = "redirectUrl=" + encodeURIComponent(redirectUrl) + "; path=/; max-age=300";
    }
});

// 2. 일반 폼 로그인 처리 함수
async function submitLogin() {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const rememberMe = document.getElementById("rememberMe").checked;
    const errorMsg = document.getElementById("loginErrorMessage");

    // 초기화
    errorMsg.style.display = "none";
    errorMsg.textContent = "";

    if (!username || !password) {
        errorMsg.textContent = "아이디와 비밀번호를 모두 입력해주세요.";
        errorMsg.style.display = "block";
        return;
    }

    try {
        const result = await Api.postRequest("/api/users/login",{
            username,
            password,
            rememberMe
       });

        if (result && result.isAvailable) {
            // 성공 시 메인이나 상품 목록으로 이동
            console.log("로그인 성공");

            const urlParams = new URLSearchParams(window.location.search);
            const redirectUrl = urlParams.get('redirectUrl');

            // URL 디코딩 처리 후 이동
            if (redirectUrl && !redirectUrl.includes('/users/login')) {
                window.location.href = decodeURIComponent(redirectUrl); // 인코딩된 URI(URL) 컴포넌트(문자열)를 원래의 문자로 복호화(디코딩)
                document.cookie = "redirectUrl=" + encodeURIComponent(redirectUrl) + "; path=/; max-age=300";
            } else {
                window.location.href= "/items/list/1"; // 없으면 메인으로
            }
        } else {
            // 컨트롤러에서 200 OK를 주면서 success: false를 보낼 경우
            errorMsg.textContent = result.message || "로그인에 실패했습니다.";
            errorMsg.style.display = "block";
        }
    } catch (error) {
        // 401 Unauthorized 등 에러 발생 시 (Api.postRequest에서 throw한 에러)
        errorMsg.textContent = "아이디 또는 비밀번호가 일치하지 않습니다.";
        errorMsg.style.display = "block";
    }
}

// 3. 폼 제출 이벤트 바인딩
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        submitLogin();
    });
}