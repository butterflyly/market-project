document.querySelector('form').onsubmit = async function(e) {
    e.preventDefault(); // 폼 제출 막기

    // 입력값 가져오기
    const title = document.getElementById('title').value;
    const content = document.getElementById('content').value;
    const price = parseInt(document.getElementById('price').value);
    const categoryId = document.getElementById('categoryId').value;
    const fileInput = document.getElementById('images'); // 파일 input

    // 클라이언트 사이드 검증
    let clientErrors = [];
    if (!title.trim()) clientErrors.push("제목은 필수 입력입니다.");
    else if (title.length < 2) clientErrors.push("제목은 최소 2글자 이상이어야 합니다.");
    if (!content.trim()) clientErrors.push("내용을 입력해주세요.");
    if (isNaN(price) || price < 0) clientErrors.push("가격은 0원 이상 숫자로 입력해주세요.");
    if (!categoryId) clientErrors.push("카테고리를 선택해주세요.");
    if (fileInput.files.length > 5) clientErrors.push("이미지는 최대 5장까지 가능합니다.");

    if (clientErrors.length > 0) {
        renderErrors(clientErrors);
        return;
    }

    //  FormData 생성 (JSON 대신 사용)
    const formData = new FormData();
    formData.append('title', title);
    formData.append('content', content);
    formData.append('price', price);
    formData.append('categoryId', categoryId);

    // 이미지 파일들 추가 (RequestDTO의 필드명 'images'와 일치해야 함)
    for (let i = 0; i < fileInput.files.length; i++) {
        formData.append('images', fileInput.files[i]);
    }

  try {
      const result = await Api.formRequest("/api/items/create", formData);
      if (result && result.isAvailable) {
          alert(result.message);
          window.location.href = `/items/list/${categoryId}`;
      }
  } catch (error) {
      if (error.message.includes("로그인")) {
          alert(error.message); window.location.href = "/users/login"; return;
      }
      renderErrors(error.message.split("\n"));
  }
};

