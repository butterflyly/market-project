// 기존 이미지 삭제 ID를 담을 배열 (글로벌 또는 적절한 스코프에 선언)
let deleteImageIds = [];

// 기존 이미지 목록을 감싸는 부모 컨테이너(또는 form 전체)에 이벤트를 바인딩
const editForm = document.getElementById('editForm');
if (editForm) {
    editForm.addEventListener('click', function(event) {
        // 클릭된 요소가 기존 이미지 카드 내부인지, 혹은 카드 자체인지 판별
        const imageCard = event.target.closest('.btn-remove-img');

        if (imageCard) {
            if (confirm("이 이미지를 삭제하시겠습니까?")) {
                // data-image-id 속성에서 정수형 ID 추출
                const imageId = imageCard.dataset.imageId;

                deleteImageIds.push(imageId); // 삭제 리스트에 추가
                imageCard.style.display = 'none'; // 화면에서 숨김
            }
        }
    });
}

document.querySelector('form').onsubmit = async function(e) {
    e.preventDefault(); // 폼 제출 막기

    // 입력값 가져오기
    const itemId = document.getElementById('itemId').value;
    const title = document.getElementById('title').value;
    const content = document.getElementById('content').value;
    const price = parseInt(document.getElementById('price').value);
    const categoryId = document.getElementById('categoryId').value;
    const fileInput = document.getElementById('images');

    // 클라이언트 사이드 검증
    let clientErrors = [];
    if (!title.trim()) clientErrors.push("제목은 필수 입력입니다.");
    else if (title.length < 2) clientErrors.push("제목은 최소 2글자 이상이어야 합니다.");

    if (!content.trim()) clientErrors.push("내용을 입력해주세요.");
    if (isNaN(price) || price < 0) clientErrors.push("가격은 0원 이상 숫자로 입력해주세요.");
        if (!categoryId) clientErrors.push("카테고리를 선택해주세요.");

    if (clientErrors.length > 0) {
        renderErrors(clientErrors);
        return;
    }

    // FormData 생성
    const formData = new FormData();
    formData.append('title', title);
    formData.append('content', content);
    formData.append('price', price);
    formData.append('categoryId', categoryId);

    // 신규 이미지 파일들 추가 (DTO의 필드명 'images'와 일치)
    if (fileInput && fileInput.files.length > 0) {
        for (let i = 0; i < fileInput.files.length; i++) {
            formData.append('images', fileInput.files[i]);
        }
    }

    // 삭제할 이미지 ID들 추가 (컨트롤러의 @RequestParam 명칭과 일치)
    deleteImageIds.forEach(id => {
        formData.append('deleteImageIds', id);
    });

  try {
      const result = await Api.formRequest(`/api/items/update/${itemId}`, formData, 'PUT');
      if (result && (result.success || result.isAvailable)) {
          alert(result.message || "상품이 수정되었습니다.");
          window.location.href = `/items/detail/${itemId}`;
      }
  } catch (error) {
      if (error.message.includes("로그인")) {
          alert(error.message); window.location.href = "/users/login"; return;
      }
      renderErrors(error.message.split("\n"));
  }
};

