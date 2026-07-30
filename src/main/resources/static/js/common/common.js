document.addEventListener("DOMContentLoaded", function () {
    // 1. 상태 체크를 먼저 실행 (내부에서 SSE 켬)
    checkLoginStatus();

    // 2. 로그아웃 버튼 이벤트 리스너 바인딩
    initLogoutEvent();
});

async function checkLoginStatus() {
    try {
        const res = await fetch("/api/users/me");

        if (res.ok) {
            const result = await res.json();

            if (result && result.isAvailable && result.data) {
                const user = result.data;

                //  엘리먼트가 존재할 때만 class와 데이터를 조작하도록 안전장치 추가
                const registerBtn = document.getElementById("nav-register");
                const loginBtn = document.getElementById("nav-login");
                const notificationMenu = document.getElementById("nav-notification");
                const logoutBtn = document.getElementById("nav-logout");
                const profileArea = document.getElementById("nav-profile-area");
                const nicknameSpan = document.getElementById("nav-profile-nickname");
                const profileImg = document.getElementById("nav-profile-img");
                const adminMenu = document.getElementById("nav-admin");

                if (registerBtn) registerBtn.classList.add("d-none");
                if (loginBtn) loginBtn.classList.add("d-none");
                if (notificationMenu) notificationMenu.classList.remove("d-none");
                if (logoutBtn) logoutBtn.classList.remove("d-none");
                if (profileArea) profileArea.classList.remove("d-none");

                // DB에 저장된 실제 최신 닉네임과 프로필로 덮어쓰기
                if (nicknameSpan) nicknameSpan.innerText = user.nickname;
                if (profileImg && user.profileUrl) profileImg.src = user.profileUrl;

                if (adminMenu && user.role === 'ADMIN') {
                    adminMenu.style.display = "block";
                }

                // 확실히 인증이 완료되었으니 SSE 알림 구독 ON
                initRealtimeNotification();
            }
        } else {
            initGuestUI();
        }
    } catch (error) {
        console.error("인증 체크 실패:", error);
        initGuestUI();
    }
}

function initGuestUI() {
    // 유저 메뉴 숨기기
    document.querySelectorAll(".auth-user").forEach(el => el.classList.add("d-none"));

    // 게스트 메뉴 켜기 (존재할 때만)
    const registerBtn = document.getElementById("nav-register");
    const loginBtn = document.getElementById("nav-login");
    if (registerBtn) registerBtn.classList.remove("d-none");
    if (loginBtn) loginBtn.classList.remove("d-none");
}

//  알림 구독 로직을 별도 함수로 격리
function initRealtimeNotification() {
    const eventSource = new EventSource("/api/notifications/subscribe");

    eventSource.addEventListener("notification", function (event) {
        const notificationData = JSON.parse(event.data);
        updateNotificationDOM(notificationData);
    });
}

// 알림 받았을 때 드롭다운 조립 함수 깔끔하게 정돈
function updateNotificationDOM(data) {
    const badge = document.getElementById("notificationBadge");
    if (badge) {
        let currentCount = parseInt(badge.innerText) || 0;
        badge.innerText = currentCount + 1;
        badge.classList.remove("d-none");
    }

    //  알림 타입별 아이콘 동적 분기 처리
    let iconClass = 'bi-bell-fill text-secondary';
    if (data.type === 'CHAT') iconClass = 'bi-chat-dots-fill text-primary';
    if (data.type === 'PRICE_DROPPED') iconClass = 'bi-arrow-down-circle-fill text-warning';

    const dropdownMenu = document.querySelector("#notificationDropdown + .dropdown-menu");
    // 아까 누락하셨던 dropdownMenu 존재 여부 체크 안전장치 추가
    if (!dropdownMenu) return;

    const emptyMsg = document.getElementById("emptyNotification");
    if (emptyMsg) emptyMsg.classList.add("d-none");

    //  내부에 하드코딩되어 번거롭게 만들던 깨진 이미지나 엑박 요소를 완벽 제거한 깨끗한 포맷
    const newAlertHtml = `
        <li>
            <a class="dropdown-item p-3 border-bottom d-flex align-items-start gap-2" href="${data.targetUrl}">
                <div class="rounded-circle p-1 flex-shrink-0" style="width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;">
                    <i class="bi ${iconClass} small"></i>
                </div>
                <div class="w-100">
                    <p class="mb-1 text-wrap small fw-bold">${data.message}</p>
                    <small class="text-muted" style="font-size: 0.75rem;">방금 전</small>
                </div>
            </a>
        </li>
    `;

    const header = dropdownMenu.querySelector(".dropdown-header");
    if (header) {
        header.insertAdjacentHTML("afterend", newAlertHtml);
    }
}

// 로그아웃 이벤트 바인딩 함수 분리
function initLogoutEvent() {

    const logoutBtn = document.getElementById("nav-logout");
    if (!logoutBtn) return;

    const logoutLink = logoutBtn.querySelector("a");
    if (!logoutLink) return;

    logoutLink.addEventListener("click", async function (event) {
           event.preventDefault(); // a 태그 기본 이동 막기

           if (!confirm("로그아웃 하시겠습니까?")) return;

           try {
              // 1. 로그아웃 처리 (쿠키 삭제)
              await Api.postRequest("/api/users/logout");

              // 2. 현재 페이지가 비인증 상태로 접근 가능한지 시큐리티에 테스트 요청
              const checkCurrentPage = await fetch(window.location.href, {
                  method: 'GET',
                  headers: { 'Accept': 'text/html' }
              });

              // 3. 시큐리티에 의해 거부되었거나(401/403) 로그인 페이지로 리다이렉트 유도된 경우
              if (checkCurrentPage.status === 401 || checkCurrentPage.status === 403 || checkCurrentPage.redirected) {
                  // 로그인 페이지로 이동 (이전 주소를 redirectUrl로 챙겨줌)
                  window.location.href = "/users/login?redirectUrl=" + encodeURIComponent(window.location.pathname);
              } else {
                  // 인증이 필요 없는 일반 페이지(상품 목록 등)는 그대로 새로고침
                  window.location.reload();
              }
           } catch (error) {
             console.error("로그아웃 처리 중 에러:", error);
             window.location.reload();
         }
    });
}