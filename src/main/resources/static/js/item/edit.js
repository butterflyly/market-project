document.addEventListener('DOMContentLoaded', function() {

    // 1. 변수 스코프 격리
    let deleteImageIds = [];
    const editForm = document.getElementById('editForm');

    if (!editForm) return;

    // 2. 이미지 삭제 클릭 이벤트 (이벤트 위임)
    editForm.addEventListener('click', function(event) {
        const imageCard = event.target.closest('.btn-remove-img');
        if (imageCard) {
            if (confirm("이 이미지를 삭제하시겠습니까?")) {
                const imageId = imageCard.dataset.imageId;
                deleteImageIds.push(imageId);
                imageCard.style.display = 'none';
            }
        }
    });

    // 3. 폼 제출 이벤트 처리
    editForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const itemId = document.getElementById('itemId').value;
        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;
        const price = parseInt(document.getElementById('price').value);
        const categoryId = document.getElementById('categoryId').value;
        const fileInput = document.getElementById('images');

        // 검증 로직
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

        const formData = new FormData();
        formData.append('title', title);
        formData.append('content', content);
        formData.append('price', price);
        formData.append('categoryId', categoryId);

        if (fileInput && fileInput.files.length > 0) {
            for (let i = 0; i < fileInput.files.length; i++) {
                formData.append('images', fileInput.files[i]);
            }
        }

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
                alert(error.message); 
                window.location.href = "/users/login"; 
                return;
            }
            renderErrors(error.message.split("\n"));
        }
    });
});