async function submitWithdraw() {

    if (typeof hideErrors === 'function') hideErrors();

    const passwordInput = document.getElementById('password');
    const password = document.getElementById('password').value;

    if (!password) {
        renderErrors(["비밀번호를 입력해주세요."]);
        passwordInput.focus();
        return;
    }

   if (!confirm("정말로 탈퇴하시겠습니까? 데이터는 정책에 따라 일정 기간 보관 후 삭제됩니다.")) {
       return;
   }

   try {

      const result = await Api.postRequest("/api/users/delete", { password }, "DELETE");

      if (result.isAvailable) {
          alert("그동안 이용해주셔서 감사합니다. 회원 탈퇴가 완료되었습니다.");
          location.href = "/"; // 메인으로 리다이렉트
      }
   } catch (error) {

      const errorList = error.message.split("\n");
      renderErrors(errorList);

      // 입력창 비우고 포커스 주기 (사용자 편의성)
      passwordInput.value = '';
      passwordInput.focus();
   }
}