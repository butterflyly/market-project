document.addEventListener("DOMContentLoaded", () => {
    // 1. 회원 탈퇴 이벤트 핸들러 통합 관리
    initWithdrawEvent();

    // 2. 내가 쓴 후기 비동기 즉시 삭제 이벤트 핸들러
    initReviewDeleteEvents();
});

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