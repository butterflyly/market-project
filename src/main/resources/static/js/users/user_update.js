//  이미지 미리보기 로직 (HTML의 onchange 이름과 일치시킴)
function previewProfileImage(input) {
    // 사용자가 파일을 선택했고, 첫 번째 파일이 존재하는지 확인
    if (input.files && input.files[0]) {
        const file = input.files[0];

        // 이미지 파일인지 유효성 검사 (선택 사항이지만 안전함)
        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 업로드 가능합니다.');
            input.value = ''; // 선택한 파일 초기화
            return;
        }

        //  브라우저 메모리에 가상 이미지 URL을 즉시 생성
        const blobUrl = URL.createObjectURL(file);

        const preview = document.getElementById('profilePreview');
        if (preview) {
            preview.src = blobUrl;

            // 메모리 누수 방지를 위해 이미지 로딩이 끝나면 가상 URL을 해제해 줍니다.
            preview.onload = function() {
                URL.revokeObjectURL(blobUrl);
            };
        }
    }
}

// 프로필 수정 요청 제출 로직
async function submitUpdate() {
     if (typeof hideErrors === 'function') hideErrors();

     const nickname = document.getElementById('nickname').value.trim();
     const fileInput = document.getElementById('profileImage');

     const clientErrors = [];
     if (!nickname) {
         clientErrors.push("닉네임을 입력해주세요.");
     } else if (nickname.length < 2 || nickname.length > 10) {
         clientErrors.push("닉네임은 2자 이상 10자 이하로 입력해주세요.");
     }

     if (clientErrors.length > 0) {
         renderErrors(clientErrors);
         return;
     }

     const formData = new FormData();
     formData.append('nickname', nickname);

     if (fileInput.files.length > 0) {
         formData.append('profileImage', fileInput.files[0]);
     }

    try {
        const result = await Api.formRequest("/api/users/update", formData, 'PUT');

        //  이전 단계에서 바꾼 백엔드 표준 규격(isAvailable)에 맞춰 조건문 수정
        if (result && result.isAvailable) {
            alert(result.message || "프로필이 수정되었습니다.");
            window.location.href = "/users/mypage";
        } else {
            renderErrors([result.message || "수정에 실패했습니다."]);
        }
    } catch (error) {
        renderErrors(error.message.split("\n"));
    }
}