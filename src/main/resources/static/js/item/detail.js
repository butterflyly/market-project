document.addEventListener('DOMContentLoaded', function() {

    const deleteBtn = document.getElementById('deleteBtn');
    const chatCreateBtn = document.getElementById('chat_create');
    const statusSelect = document.getElementById('statusSelect');
    const likeBtn = document.getElementById('likeBtn');
    const itemId = document.getElementById('itemId').value;

    if (!itemId) {
        console.error("게시글 정보를 찾을 수 없습니다.");
        return;
    }

    // [1] 게시글 삭제 이벤트
    if (deleteBtn) {
        deleteBtn.addEventListener('click', async function() {
            //  실수 방지를 위한 컨펌창
            if (!confirm("정말로 이 게시글을 삭제하시겠습니까?\n삭제 후에는 복원할 수 없습니다.")) {
                return;
            }

            try {
                const result = await Api.postRequest(`/api/items/delete/${itemId}`, {},"DELETE");

                if (result && result.isAvailable) {
                    alert(result.message || "성공적으로 삭제되었습니다.");
                    //  삭제 완료 후 목록으로 리다이렉트
                    window.location.href = "/items/list/1";
                }
            } catch (error) {
                console.error("삭제 실패:", error);
                alert(error.message || "삭제 처리 중 오류가 발생했습니다.");
            }
        });
    }

    // [2] 채팅방 생성 이벤트
    if (chatCreateBtn) {
        chatCreateBtn.addEventListener('click', async function() {

        const id = chatCreateBtn.dataset.itemId || itemId;

        try
        {
            const result = await Api.postRequest(`/api/chat-room/create?itemId=${id}`);

            if (result.isAvailable) {

                alert(result.message);


                const roomId = result.data;
                window.location.href = `/chat/room/${roomId}`;
            }
        } catch (error) {
            console.error("채팅방 생성 실패:", error);
            alert(error.message || "채팅방을 생성할 수 없습니다.");
        }
     });
   }

   // [3] 판매 상태 변경 로직
   if (statusSelect) {

          // 로직 전의 판매 상태
          let previousStatus = statusSelect.value;

          // 화면에 모달을 띄우기 위한 자바스크립트 문법적 안전장치
          const buyerModalEl = document.getElementById("buyerSelectModal");

          // 일반 구매자가 이 페이지를 열면? -> 방어로직
          const buyerModal = buyerModalEl ? new bootstrap.Modal(buyerModalEl) : null;
          const buyerListContainer = document.getElementById("buyerListContainer");

         // 구매자 선택 모달을 닫을 경우 원복
         document.getElementById("buyerModalCloseBtn")?.addEventListener("click", () => {
             statusSelect.value = previousStatus;
         });


         // 최종 안전망 (아무도 안 고르고 모달 닫았을 때 원래 상태로 원복)
         buyerModalEl?.addEventListener("hidden.bs.modal", () => {
             // 예약중(RESERVED)이나 판매완료(SOLD_OUT) 상태를 시도하다가 구매자 안 고르고 꺼졌을 때 검사
             if (statusSelect.value === "SOLD_OUT" || statusSelect.value === "RESERVED") {
                 statusSelect.value = previousStatus;
             }
         });

          // 변경 이벤트 동작
          statusSelect.addEventListener("change", async (e) => {

             const currentStatus = e.target.value;

             // 현재 상태가 예약중인지 확인하는 플래그와 동적 문구 세팅
             const isReservedAction = currentStatus === "RESERVED";
             const statusText = isReservedAction ? "예약" : "거래 완료";
             const infoText = isReservedAction ? "확인 시 해당 유저와 예약 상태로 변경됩니다." : "확인 시 변경 후 되돌릴 수 없습니다.";

             // 현재 DB 상태가 RESERVED(예약중)인데 SOLD_OUT(판매완료)으로 바꾸려고 하는지 체크
             const isCurrentlyReserved = previousStatus === "RESERVED";

             //  판매완료(SOLD_OUT) 또는 예약중(RESERVED)을 선택할 경우 구매자 검증 프로세스 가동
              if (currentStatus === "SOLD_OUT" || currentStatus === "RESERVED") {

                   // 이미 예약중인 상품을 판매완료로 전환할 때 (구매자 다수 조회를 건너뜀)
                   if (isCurrentlyReserved && currentStatus === "SOLD_OUT") {
                       if (confirm("기존 예약자분과 거래를 완료하시겠습니까?\n확인 시 바로 판매완료 처리됩니다.")) {
                           // buyerUsername 자리에 null을 보내거나, 백엔드가 기존 buyer_id를 유지하도록 처리
                           await requestUpdateStatus(itemId, currentStatus, null);
                       } else {
                           statusSelect.value = previousStatus;
                       }
                       return;
                   }

                   try {
                       const response = await Api.postRequest(`/api/items/${itemId}/chat-buyers`, null, "GET");

                       // 백엔드 시스템 자체가 터졌는지 검사 (빈 리스트는 통과됨)
                       if (!response.isAvailable) {
                           alert(response.message || "구매자 목록을 불러오지 못했습니다.");
                           statusSelect.value = previousStatus;
                           return;
                       }

                       const buyers = response.data; // List<ChatBuyerResponse>

                      // 대화 중인 구매자가 아무도 없을 때
                      if (!buyers || buyers.length === 0) {
                           alert("이 상품으로 대화한 구매자가 없어 '판매완료' 상태로 변경할 수 없습니다.");
                           statusSelect.value = previousStatus; // 셀렉트 박스를 원래 상태로 원복
                           return;
                      }

                      // 대화자가 딱 1명일 때
                      if (buyers.length === 1) {
                           const buyer = buyers[0];

                           if (confirm(`'${buyer.displayNickname}'님과 ${statusText}하시겠습니까?\n${infoText}`)) {

                               await requestUpdateStatus(itemId, currentStatus, buyer.username);
                           } else {
                               statusSelect.value = previousStatus;
                           }
                           return;
                      }

                      // 채팅방 인원이 다수일 경우
                      if (buyers.length > 1) {

                           buyerListContainer.innerHTML = "";

                           buyers.forEach(buyer => {

                           // 버튼값 변경
                           const btn = document.createElement("button");
                           btn.type = "button";
                           btn.className = "btn btn-outline-primary text-start d-flex align-items-center justify-content-between p-2 fw-medium";
                           btn.innerHTML = `
                               <span>${buyer.displayNickname} <small class="text-muted">(${buyer.username})</small></span>
                               <i class="bi bi-chevron-right small"></i>
                           `;

                           btn.addEventListener("click", async () => {
                               buyerModal.hide();
                               await requestUpdateStatus(itemId, currentStatus, buyer.username);
                           });

                           buyerListContainer.appendChild(btn);
                           });

                           buyerModal.show();
                      }
                   } catch (error) {
                       console.error(error);
                       alert(error.message || "서버 통신 중 오류가 발생했습니다.");
                       statusSelect.value = previousStatus;
                   }
               }

                // 판매중(ON_SALE)이나 거래 취소(CANCELED) 등으로 바꿀 경우 (구매자 지정 필요 없음)
               else {
                   if (confirm("상품 상태를 변경하시겠습니까?")) {
                       await requestUpdateStatus(itemId, currentStatus, null);
                   } else {
                       statusSelect.value = previousStatus;
                   }
               }
           });

           async function requestUpdateStatus(itemId, targetStatus, buyerUsername) {

               try {
                   const bodyData = {
                       itemStatus: targetStatus,
                       buyerUsername: buyerUsername
                   };

                   const response = await Api.postRequest(`/api/items/status/${itemId}`, bodyData, "PATCH");

                   if (response.isAvailable) {
                       alert(response.message || "상태가 성공적으로 변경되었습니다.");
                       previousStatus = targetStatus;
                       updateSelectStyle(statusSelect, targetStatus); // 새로고침 전 스타일 즉시 반영 가능
                       location.reload();
                   } else {
                       alert(response.message || "변경에 실패했습니다.");
                       statusSelect.value = previousStatus;
                   }
               } catch (error) {
                   console.error(error);
                   alert(error.message || "요청 처리 중 오류가 발생했습니다.");
                   statusSelect.value = previousStatus;
               }
           }
       }

       // [4] 찜(좋아요) 토글 이벤트 추가
       if (likeBtn) {
           likeBtn.addEventListener('click', async function() {
               try {

                   const result = await Api.postRequest(`/api/items/vote/${itemId}`);

                   if (result && result.isAvailable) {
                       const likeIcon = document.getElementById('likeIcon');
                       const likeCountText = document.getElementById('likeCountText');
                       let currentCount = parseInt(likeCountText.innerText);

                       // 화면단 실시간 토글 이펙트 (아이콘 클래스 분기)
                       if (likeIcon.classList.contains('bi-heart')) {
                           // 1. 찜 등록 상태로 변경
                           likeIcon.classList.remove('bi-heart');
                           likeIcon.classList.add('bi-heart-fill');
                           likeCountText.innerText = currentCount + 1;
                           likeBtn.classList.remove('btn-outline-danger');
                           likeBtn.classList.add('btn-danger'); // 빨간색 채우기
                       } else {
                           // 2. 찜 취소 상태로 변경
                           likeIcon.classList.remove('bi-heart-fill');
                           likeIcon.classList.add('bi-heart');
                           likeCountText.innerText = Math.max(0, currentCount - 1);
                           likeBtn.classList.remove('btn-danger');
                           likeBtn.classList.add('btn-outline-danger'); // 라인만 남기기
                       }
                   }
               } catch (error) {
                   console.error("찜 처리 실패:", error);
                   alert(error.message || "로그인이 필요하거나 찜을 처리할 수 없습니다.");
               }
           });
       }

       // [5] 신고 폼 제출 이벤트
       document.getElementById('reportForm')?.addEventListener('submit', async function(e) {

           e.preventDefault();

           const reportType = document.getElementById('reportType').value;
           const content = document.getElementById('reportContent').value;

           if(!confirm("정말로 이 게시글을 신고하시겠습니까?")) return;

           try {
               const result = await Api.postRequest(`/api/reports/item/${itemId}`, {
                   reportType: reportType,
                   content: content
               });

               if (result && result.isAvailable) {
                   alert("신고가 정상적으로 접수되었습니다. 관리자 검토까지 시일이 소요될 수 있습니다.");

                   const modalEl = document.getElementById('reportModal');
                   const modal = bootstrap.Modal.getInstance(modalEl);
                   modal?.hide();

                   document.getElementById('reportForm').reset();
               } else {
                   alert(result.message || "신고 처리 중 에러가 발생했습니다.");
               }
           } catch (error) {
               console.error("신고 실패:", error);
               alert(error.message || "서버 통신 오류가 발생했습니다.");
           }
       });
});

/**
 * 셀렉트 박스 선택 값에 따라 테두리색과 글자색을 동적으로 교체하는 UI 헬퍼 함수
 */
function updateSelectStyle(selectElement, status) {
    // 기존 스타일 클래스 제거
    selectElement.classList.remove('border-primary', 'text-primary', 'border-warning', 'text-warning', 'border-secondary', 'text-secondary');

   if (status === 'ON_SALE') {
       selectElement.classList.add('border-primary', 'text-primary');
   } else if (status === 'RESERVED') {
       selectElement.classList.add('border-warning', 'text-warning');
   } else if (status === 'SOLD_OUT') {
       selectElement.classList.add('border-secondary', 'text-secondary');
   }
}


