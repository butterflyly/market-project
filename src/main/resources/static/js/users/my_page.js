document.addEventListener("DOMContentLoaded", () => {
    // 1. 회원 탈퇴 이벤트 핸들러 통합 관리
    initWithdrawEvent();

    // 2. 내가 쓴 후기 비동기 즉시 삭제 이벤트 핸들러
    initReviewDeleteEvents();

    // 3. 리뷰 작성 모달 이벤트 및 URL 파라미터 감지 추가
    initReviewModalEvents();

    // 4. URL 파라미터 감지 후 필요 시 모달 오픈
    checkUrlForReviewModal();
});


/**
 * 리뷰 작성 모달 열기 공통 함수
 */
function openReviewModal(itemId, itemTitle) {
    const itemIdInput = document.getElementById('modalItemId');
    const itemTitleSpan = document.getElementById('modalItemTitle');

    if (!itemIdInput || !itemTitleSpan) {
        console.error("리뷰 모달 요소를 찾을 수 없습니다.");
        return;
    }

    itemIdInput.value = itemId;
    itemTitleSpan.innerText = itemTitle || "구매 상품";
    document.getElementById('modalContent').value = ''; // 초기화
    document.getElementById('modalRating').value = '5'; // 기본 5점

    const modalEl = document.getElementById('reviewModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

/**
 * 리뷰 모달 관련 이벤트 연동 (열기 이벤트 위임 + 등록 버튼 클릭)
 */
function initReviewModalEvents() {
    // [이벤트 위임] 후기 작성 버튼 클릭 감지 (th:onclick 제거에 따른 처리)
    document.addEventListener('click', function (event) {
        const btn = event.target.closest('.btn-open-review-modal');
        if (btn) {
            const itemId = btn.dataset.itemId;
            const itemTitle = btn.dataset.itemTitle;
            openReviewModal(itemId, itemTitle);
        }
    });

    // 후기 등록 완료 버튼 클릭 이벤트
    const submitBtn = document.getElementById('submitReviewBtn');
    if (!submitBtn) return;

    submitBtn.addEventListener('click', async () => {
        const itemId = document.getElementById('modalItemId').value;
        const rating = document.getElementById('modalRating').value;
        const content = document.getElementById('modalContent').value.trim();

        if (!content) {
            alert("후기 내용을 입력해주세요.");
            return;
        }

        try {
            const response = await Api.postRequest(`/api/reviews/item/${itemId}`, {
                rating: parseInt(rating),
                content: content
            });

            if (response && response.isAvailable) {
                alert(response.message || "거래 후기가 성공적으로 등록되었습니다!");
                location.href = "/users/mypage";
            } else {
                alert(response?.message || "후기 등록에 실패했습니다.");
            }
        } catch (error) {
            console.error("리뷰 등록 중 에러:", error);
            alert("서버와의 통신 오류가 발생했습니다.");
        }
    });
}


/**
 *  URL 파라미터 감지 (알림 클릭 시 ?reviewModal=true&itemId=XX 로 넘어왔을 때 자동 오픈)
 */
function checkUrlForReviewModal() {
    const urlParams = new URLSearchParams(window.location.search);
    const isReviewModal = urlParams.get('reviewModal');
    const itemId = urlParams.get('itemId');

    if (isReviewModal === 'true' && itemId) {
        // '내 구매 내역' 탭을 활성화시킴
        const buyTab = document.getElementById('buy-item-tab');
        if (buyTab) {
            const tab = new bootstrap.Tab(buyTab);
            tab.show();
        }
        // 해당 itemId로 모달 오픈
        openReviewModal(itemId, "거래 상품");
    }
}


/**
 * 회원 탈퇴 통합 핸들러 (일반 유저 vs 소셜 유저 분기)
 */
function initWithdrawEvent() {
    const withdrawBtn = document.getElementById('withdrawBtn');

    //  버튼이 없다? = 일반 유저라는 뜻
    if (!withdrawBtn) return;

    // 버튼이 존재한다? = 무조건 소셜 유저
    withdrawBtn.addEventListener('click', async function (event) {
        event.preventDefault();

        if (!confirm("소셜 연동 회원입니다. 정말로 탈퇴하시겠습니까?\n탈퇴 시 소셜 계정 연동이 자동으로 해제됩니다.")) {
            return;
        }

        try {
            const result = await Api.postRequest("/api/users/delete", { password: "SOCIAL_USER_WITHDRAW" }, "DELETE");

            if (result && result.isAvailable) {
                alert("그동안 이용해주셔서 감사합니다. 회원 탈퇴가 완료되었습니다.");
                location.href = "/";
            } else {
                alert(result.message || "탈퇴 처리 중 오류가 발생했습니다.");
            }
        } catch (error) {
            console.error("소셜 탈퇴 통신 실패:", error);
            alert(error.message || "통신 실패");
        }
    });
}

/**
 * 내가 쓴 후기 삭제 핸들러
 */
function initReviewDeleteEvents() {
    document.querySelectorAll('.delete-review-btn').forEach(button => {
        button.addEventListener('click', function() {
            // 타임리프 렌더링 후 브라우저 표준 규격인 dataset에서 깨끗하게 ID를 추출
            const reviewId = this.dataset.reviewId;

            if (!reviewId) {
                alert("리뷰 식별자를 찾을 수 없습니다.");
                return;
            }

            if (!confirm("이 후기를 정말로 삭제하시겠습니까?")) return;

            fetch(`/reviews/${reviewId}`, {
                method: 'DELETE'
            })
            .then(res => {
                if (!res.ok) throw new Error("서버 응답 실패");
                return res.json();
            })
            .then(data => {
                // 백엔드 표준 리턴 규격(isAvailable) 검증 반영 가능구역
                // 만약 리뷰 컨트롤러가 이전 표준인 success를 쓴다면 원래 코드대로 유지
                if (data.success || data.isAvailable || data === true) {
                    alert("후기가 정상적으로 삭제되었습니다.");
                    location.reload();
                } else {
                    alert(data.message || "삭제에 실패했습니다.");
                }
            })
            .catch(err => {
                console.error(err);
                alert("서버 오류로 인해 삭제에 실패했습니다.");
            });
        });
    });
}