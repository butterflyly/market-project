document.addEventListener("DOMContentLoaded", () => {
    const passwordForm = document.getElementById("passwordForm");
    const currentInput = document.getElementById("currentPassword");
    const newPasswordInput = document.getElementById("newPassword");
    const confirmInput = document.getElementById("confirmNewPassword");

    const newPasswordError = document.getElementById("newPasswordError");
    const confirmError = document.getElementById("confirmPasswordError");

    let isNewPasswordValid = false;
    let isConfirmValid = false;

    // 1. 새 비밀번호 정규식 체크
    newPasswordInput.addEventListener("input", () => {
        const password = newPasswordInput.value;
        const regex = /^(?=.*[a-zA-Z])(?=.*\d).{12,}$/;

        if (!regex.test(password)) {
            newPasswordInput.classList.add("error-input");
            newPasswordError.textContent = "영문 + 숫자 포함 12자 이상이어야 합니다.";
            newPasswordError.style.display = "block";
            isNewPasswordValid = false;
        } else {
            newPasswordInput.classList.remove("error-input");
            newPasswordError.style.display = "none";
            isNewPasswordValid = true;
        }
    });

    // 2. 새 비밀번호 확인 일치 체크
    confirmInput.addEventListener("input", () => {
        if (newPasswordInput.value !== confirmInput.value) {
            confirmInput.classList.add("error-input");
            confirmError.textContent = "비밀번호가 일치하지 않습니다.";
            confirmError.style.display = "block";
            isConfirmValid = false;
        } else {
            confirmInput.classList.remove("error-input");
            confirmError.style.display = "none";
            isConfirmValid = true;
        }
    });

    // 3. 최종 제출 처리
    passwordForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        if (typeof hideErrors === 'function') hideErrors();

        // 프론트엔드 1차 방어
        if (!isNewPasswordValid || !isConfirmValid) {
            renderErrors(["비밀번호 형식을 다시 확인해주세요."]);
            return;
        }

        if (!currentInput.value) {
            renderErrors(["현재 비밀번호를 입력해주세요."]);
            return;
        }

        try {
            const result = await Api.postRequest("/api/users/update-password", {
                currentPassword: currentInput.value,
                newPassword: newPasswordInput.value,
                confirmNewPassword: confirmInput.value
            }, "PUT");

            if (result.isAvailable) {
                alert("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
                location.href = "/users/login";
            }
        } catch (error) {
            // 서버 BusinessException 처리 (현재 비밀번호 불일치 등)
            const errorList = error.message.split("\n");
            renderErrors(errorList);
        }
    });
});