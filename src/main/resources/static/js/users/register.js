// 전역 변수 (상태 관리)
let duplicateChecked = false;
let duplicateNicknameChecked = false;
let duplicateEmailChecked = false;
let mailAuthNumberChecked = false;
let Passwordform = false;
let PasswordCheck = false;

const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;

// 이미지 미리보기 함수
function previewImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('profilePreview');
            if (preview) preview.src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    }
}

/**
 * UI 상태 업데이트 공통 함수
 * @param {string} fieldId - input 요소의 ID
 * @param {boolean} isSuccess - 성공 여부
 * @param {string} message - 표시할 메시지
 */
function updateFieldStatus(fieldId, isSuccess, message) {
    const input = document.getElementById(fieldId);
    const errorMsg = document.getElementById(`${fieldId}-error`);

    const camelId = fieldId.charAt(0).toUpperCase() + fieldId.slice(1);
    const checkBtn = document.getElementById(`duplicateCheck${camelId}Button`);
    const changeBtn = document.getElementById(`change${camelId}Button`);

    errorMsg.textContent = message;
    errorMsg.className = isSuccess ? "msg-success" : "msg-error";
    errorMsg.style.display = "block";

    if (isSuccess) {
        input.readOnly = true;
        if (checkBtn) checkBtn.style.display = "none";
        if (changeBtn) changeBtn.style.display = "inline";
    } else {
        input.readOnly = false;
        if (checkBtn) checkBtn.style.display = "inline";
        if (changeBtn) changeBtn.style.display = "none";
    }
}

// DOM 준비 완료 시 이벤트 바인딩 일괄 처리
document.addEventListener('DOMContentLoaded', function () {

    // 1. 버튼 클릭 이벤트 바인딩
    document.getElementById('duplicateCheckUsernameButton').addEventListener('click', checkDuplicateUsername);
    document.getElementById('changeUsernameButton').addEventListener('click', changeUsername);

    document.getElementById('duplicateCheckNicknameButton').addEventListener('click', checkDuplicateNickname);
    document.getElementById('changeNicknameButton').addEventListener('click', changeNickname);

    document.getElementById('duplicateCheckEmailButton').addEventListener('click', sendEmail);
    document.getElementById('changeEmailButton').addEventListener('click', changeEmail);

    document.getElementById('duplicateCheckInputCodeButton').addEventListener('click', verifyCode);
    document.getElementById('submitRegisterButton').addEventListener('click', submitRegister);

    document.getElementById('profileUploadTrigger').addEventListener('click', () => {
            document.getElementById('profileImage').click();
        });

    // 2. 실시간 입력 변수 선언
    const usernameInput = document.getElementById("username");
    const nicknameInput = document.getElementById("nickname");
    const emailInput = document.getElementById("email");
    const inputCode = document.getElementById("inputCode");
    const passwordInput = document.getElementById("passwordInput");
    const passwordConfirmInput = document.getElementById("passwordConfirm");

    const passwordError = document.getElementById("passwordInput-error");
    const passwordConfirmError = document.getElementById("passwordConfirm-error");


    const inputs = [usernameInput, nicknameInput, emailInput, inputCode, passwordInput, passwordConfirmInput];

    inputs.forEach(input => {
        if (input) { // 혹시나 null인 경우를 대비한 안전장치
            input.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    event.preventDefault(); // 엔터키 누를 때 폼이 멋대로 서브밋되어 새로고침되는 현상 방지
                    submitRegister(); // 최종 회원가입 함수 호출!
                }
            });
        }
    });

    // 3. 실시간 입력 감지 이벤트 바인딩
    usernameInput.addEventListener("input", () => { duplicateChecked = false; });
    nicknameInput.addEventListener("input", () => { duplicateNicknameChecked = false; });
    emailInput.addEventListener("change", () => { duplicateEmailChecked = false; mailAuthNumberChecked = false; });

    // 비밀번호 실시간 형식 검증
    passwordInput.addEventListener("input", () => {
        const password = passwordInput.value;
        const regex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#$%^&*()+|=]).{8,16}$/;

        if (!regex.test(password)) {
            passwordInput.classList.add("error-input");
            Passwordform = false;
            passwordError.textContent = "비밀번호는 영문 + 숫자 포함 8 ~ 16글자 이어야 합니다.";
            passwordError.style.display = "block";
            passwordError.className = "msg-error";
        } else {
            passwordInput.classList.remove("error-input");
            Passwordform = true;
            passwordError.style.display = "none";
        }
    });

    // 비밀번호 확인 일치 실시간 검증
    passwordConfirmInput.addEventListener("input", () => {
        if (passwordInput.value !== passwordConfirmInput.value) {
            passwordConfirmInput.classList.add("error-input");
            PasswordCheck = false;
            passwordConfirmError.textContent = "비밀번호가 일치하지 않습니다.";
            passwordConfirmError.style.display = "block";
            passwordConfirmError.className = "msg-error";
        } else {
            passwordConfirmInput.classList.remove("error-input");
            PasswordCheck = true;
            passwordConfirmError.style.display = "none";
        }
    });

});

// 아이디 중복 확인
async function checkDuplicateUsername() {
    const usernameInput = document.getElementById("username");
    const username = usernameInput.value.trim();

    if (!/^[a-zA-Z0-9]{6,15}$/.test(username)) {
        updateFieldStatus("username", false, "아이디는 영문/숫자 6~15자리로 입력해주세요.");
        duplicateChecked = false;
        return;
    }

   try {
       await Api.postRequest("/api/users/register/check-duplicate-id", { username });
       updateFieldStatus("username", true, "사용 가능한 아이디입니다.");
       duplicateChecked = true;
   } catch (error) {
       console.error("ID 중복 확인 에러:", error);
       const serverErrorMessage = error.message.replace('Error: ', '');
       updateFieldStatus("username", false, serverErrorMessage || "이미 사용 중인 아이디입니다.");
       duplicateChecked = false;
   }
}

// 닉네임 중복 확인
async function checkDuplicateNickname() {
    const nickname = document.getElementById("nickname").value.trim();

    if (!/^[a-zA-Z0-9ㄱ-ㅣ가-힣]{2,10}$/.test(nickname)) {
        updateFieldStatus("nickname", false, "닉네임은 2~10자리로 입력해주세요.");
        duplicateNicknameChecked = false;
        return;
    }

    try {
        await Api.postRequest("/api/users/register/check-duplicate-nickname", { nickname });
        updateFieldStatus("nickname", true, "사용 가능한 닉네임입니다.");
        duplicateNicknameChecked = true;
    } catch (error) {
        console.error("닉네임 중복 확인 에러:", error);
        const serverErrorMessage = error.message.replace('Error: ', '');
        updateFieldStatus("nickname", false, serverErrorMessage || "이미 사용 중인 닉네임입니다.");
        duplicateNicknameChecked = false;
    }
}

// 이메일 인증번호 전송
async function sendEmail() {
    const email = document.getElementById("email").value.trim();

    if (!emailRegex.test(email)) {
        updateFieldStatus("email", false, "이메일 형식을 확인해주세요.");
        duplicateEmailChecked = false;
        return;
    }

    try {
        await Api.postRequest("/api/users/register/mail-auth", { email });
        updateFieldStatus("email", true, "인증번호가 전송되었습니다.");
        duplicateEmailChecked = true;
    } catch (error) {
        console.error("이메일 전송 에러:", error);
        const serverErrorMessage = error.message.replace('Error: ', '');
        updateFieldStatus("email", false, serverErrorMessage || "이미 요청했거나 잠시 후 다시 시도해주세요.");
        duplicateEmailChecked = false;
    }
}

// 인증번호 확인
async function verifyCode() {
    const inputCode = document.getElementById("inputCode").value.trim();
    const email = document.getElementById("email").value.trim();

    if (!/^\d{6}$/.test(inputCode)) {
        updateFieldStatus("inputCode", false, "인증번호 6자리를 입력해주세요.");
        mailAuthNumberChecked = false;
        return;
    }

    try {
        await Api.postRequest("/api/users/register/mail-verify", { email, code: inputCode });
        updateFieldStatus("inputCode", true, "인증되었습니다.");
        mailAuthNumberChecked = true;
    } catch (error) {
        console.error("인증번호 확인 에러:", error);
        const serverErrorMessage = error.message.replace('Error: ', '');
        updateFieldStatus("inputCode", false, serverErrorMessage || "인증번호가 일치하지 않습니다.");
        mailAuthNumberChecked = false;
    }
}

// 변경 버튼 함수들
function changeUsername() {
    updateFieldStatus("username", false, "");
    duplicateChecked = false;
}

function changeNickname() {
    updateFieldStatus("nickname", false, "");
    duplicateNicknameChecked = false;
}

function changeEmail() {
    updateFieldStatus("email", false, "");
    duplicateEmailChecked = false;
    mailAuthNumberChecked = false;

    const inputCode = document.getElementById("inputCode");
    inputCode.value = "";
    updateFieldStatus("inputCode", false, "");
}

// 최종 폼 검증
function validateForm() {
   const errors = [];

   if (!duplicateChecked) errors.push("아이디 중복 확인을 완료해 주세요.");
   if (!Passwordform) errors.push("비밀번호 양식(영문+숫자 12자 이상)을 지켜주세요.");
   if (!PasswordCheck) errors.push("비밀번호가 서로 일치하지 않습니다.");
   if (!duplicateNicknameChecked) errors.push("닉네임 중복 확인을 완료해 주세요.");
   if (!duplicateEmailChecked) errors.push("이메일 인증번호를 먼저 전송해 주세요.");
   if (!mailAuthNumberChecked) errors.push("이메일 인증번호 확인이 완료되지 않았습니다.");

   if (errors.length > 0) {
       // 상단 에러 컨테이너에 바인딩하는 임시 렌더러 구현 (필요시 커스텀 함수로 대체 가능)
       const errorContainer = document.getElementById("error-container");
       const errorMessages = document.getElementById("error-messages");
       if (errorMessages && errorContainer) {
           errorMessages.innerHTML = errors.map(err => `<li>${err}</li>`).join("");
           errorContainer.classList.remove("d-none");
       }
       return false;
   }
   return true;
}

// 가입 제출
async function submitRegister() {
   if (!validateForm()) return;

   const formData = new FormData();
   formData.append('username', document.getElementById('username').value);
   formData.append('password', document.getElementById('passwordInput').value);
   formData.append('password2', document.getElementById('passwordConfirm').value);
   formData.append('nickname', document.getElementById('nickname').value);
   formData.append('email', document.getElementById('email').value);
   formData.append('inputCode', document.getElementById('inputCode').value);

   const profileFile = document.getElementById('profileImage').files[0];

   if (profileFile) {
       formData.append('profileImage', profileFile);
   }

   try {
       const result = await Api.formRequest("/api/users/register", formData);
       console.log("가입 성공 응답 데이터:", result);

       if (result && result.isAvailable) {
           alert("회원가입을 축하합니다!");
           window.location.href = "/users/login";
       }
   } catch (error) {
       console.error("가입 실패 원인 에러 객체:", error);

       const errorContainer = document.getElementById("error-container");
       const errorMessages = document.getElementById("error-messages");

       if (errorMessages && errorContainer) {
          // 기존 스타일 유지를 위해 <li> 태그로 렌더링
          errorMessages.innerHTML = `<li>${error.message.replace('Error: ', '')}</li>`;
          errorContainer.classList.remove("d-none"); // 숨겨진 에러창 등장
          window.scrollTo(0, 0); // 화면을 상단으로 올려 에러를 인지시킴
       }

   }

}