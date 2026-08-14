document.addEventListener('DOMContentLoaded', function() {
    
    const withdrawForm = document.getElementById('withdrawForm');
    
    if (withdrawForm) {
        // 인라인 onclick 대신 폼 내부 submit 이벤트를 인터셉트
        withdrawForm.addEventListener('submit', async function(e) {
            e.preventDefault(); // 브라우저 기본 제출(새로고침) 막기

            if (typeof hideErrors === 'function') hideErrors();

            const passwordInput = document.getElementById('password');
            const password = passwordInput ? passwordInput.value : '';

            // 클라이언트 사이드 기본 검증
            if (!password) {
                renderErrors(["비밀번호를 입력해주세요."]);
                if (passwordInput) passwordInput.focus();
                return;
            }

            // 탈퇴 재확인 얼럿
            if (!confirm("정말로 탈퇴하시겠습니까? 데이터는 정책에 따라 일정 기간 보관 후 삭제됩니다.")) {
                return;
            }

            try {
                // 백엔드로 DELETE 요청 전송
                const result = await Api.postRequest("/api/users/delete", { password }, "DELETE");

                if (result && result.isAvailable) {
                    alert("그동안 이용해주셔서 감사합니다. 회원 탈퇴가 완료되었습니다.");
                    window.location.href = "/"; // 메인 페이지로 리다이렉트
                }
            } catch (error) {
                const errorList = error.message.split("\n");
                renderErrors(errorList);

                // 에러 발생 시 입력창 비우고 포커스 잡아주는 사용자 편의 기능 유지
                if (passwordInput) {
                    passwordInput.value = '';
                    passwordInput.focus();
                }
            }
        });
    }
});