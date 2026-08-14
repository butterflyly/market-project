document.addEventListener('DOMContentLoaded', () => {
    initReviewDeleteEvents();
    initReviewEditEvents();
});

/**
 * 1. 리뷰 삭제 이벤트 연동 (DELETE /api/reviews/{reviewId})
 */
function initReviewDeleteEvents() {
    document.querySelectorAll('.btn-delete-review').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const reviewCard = e.target.closest('[data-review-id]');
            const reviewId = reviewCard?.dataset.reviewId;

            if (!reviewId) return;

            if (!confirm("정말 이 후기를 삭제하시겠습니까?")) return;

            try {
                // Api.postRequest(url, data, method) 활용
                const result = await Api.postRequest(`/api/reviews/${reviewId}`, null, "DELETE");

                if (result && result.isAvailable) {
                    alert(result.message || "후기가 삭제되었습니다.");
                    reviewCard.remove(); // 화면에서 요소 즉시 제거
                }
            } catch (error) {
                console.error("리뷰 삭제 에러:", error);
                alert(error.message || "리뷰 삭제에 실패했습니다.");
            }
        });
    });
}

/**
 * 2. 리뷰 수정 모달 오픈 및 요청 이벤트 연동 (PATCH /api/reviews/{reviewId})
 */
let currentEditReviewId = null;

function initReviewEditEvents() {
    // [수정] 버튼 클릭 시 모달 열기 & 기존 데이터 세팅
    document.querySelectorAll('.btn-edit-review').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const reviewCard = e.target.closest('[data-review-id]');
            currentEditReviewId = reviewCard?.dataset.reviewId;

            // 기존 카드에서 별점, 내용 끌어오기
            const currentContent = reviewCard.querySelector('.review-content-text')?.textContent.trim() || '';
            const ratingStars = reviewCard.querySelector('.text-warning')?.textContent.trim().length || 5;

            // 모달 인풋 세팅
            document.getElementById('editModalRating').value = ratingStars;
            document.getElementById('editModalContent').value = currentContent;

            // 부트스트랩 모달 띄우기
            const editModal = new bootstrap.Modal(document.getElementById('reviewEditModal'));
            editModal.show();
        });
    });

    // 모달 내 [수정 완료] 저장 버튼 클릭
    const submitEditBtn = document.getElementById('submitEditReviewBtn');
    if (submitEditBtn) {
        submitEditBtn.addEventListener('click', async () => {
            if (!currentEditReviewId) return;

            const rating = document.getElementById('editModalRating').value;
            const content = document.getElementById('editModalContent').value.trim();

            if (!content) {
                alert("내용을 입력해주세요.");
                return;
            }

            try {
                // Api.postRequest(url, data, method) 활용 -> PATCH 메서드
                const result = await Api.postRequest(`/api/reviews/${currentEditReviewId}`, {
                    rating: parseInt(rating),
                    content: content
                }, "PATCH");

                if (result && result.isAvailable) {
                    alert(result.message || "후기가 수정되었습니다.");
                    location.reload(); // 변경 내용 반영을 위해 새로고침
                }
            } catch (error) {
                console.error("리뷰 수정 에러:", error);
                alert(error.message || "리뷰 수정에 실패했습니다.");
            }
        });
    }
}