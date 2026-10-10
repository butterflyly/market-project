let stompClient = null;
let lastRenderedDate = null; // 날짜 구분선 추적용 변수

document.addEventListener('DOMContentLoaded', function() {

    // dayjs 한국어 설정
    if (typeof dayjs !== 'undefined') {
        dayjs.locale('ko');
    }

    const roomId = document.getElementById('roomId').value;

    // 웹소켓 및 SockJS 초기화 가동
    initWebSocket(roomId);

    // 요소들 가져오기
    const imageUploadBtn = document.getElementById('image-upload');
    const chatImageInput = document.getElementById('chat-image-input');
    const chatForm = document.getElementById('chat-form');
    const messageInput = document.getElementById('message-input');

    const previewContainer = document.getElementById('image-preview-container');
    const previewImage = document.getElementById('image-preview');
    const cancelImageBtn = document.getElementById('cancel-image-btn');

    let selectedFile = null;

    // 1. 이미지 버튼 클릭 시 기존 input 초기화 후 창 열기
    if (imageUploadBtn && chatImageInput) {
        imageUploadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            chatImageInput.value = '';
            chatImageInput.click();
        });
    }

    // 2. 파일 선택 변경 감지
    if (chatImageInput) {
        chatImageInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (!file) return;

            if (file.size > 5 * 1024 * 1024) {
                alert("파일 크기는 5MB 이하만 가능합니다.");
                this.value = '';
                return;
            }

            selectedFile = file;

            const reader = new FileReader();
            reader.onload = function(event) {
                if (previewImage && previewContainer) {
                    previewImage.src = event.target.result;
                    previewContainer.classList.remove('d-none');
                }
            };
            reader.readAsDataURL(file);
        });
    }

    // 3. 미리보기 X 버튼 클릭 시 초기화
    if (cancelImageBtn) {
        cancelImageBtn.addEventListener('click', function() {
            clearSelectedImage();
        });
    }

    function clearSelectedImage() {
        selectedFile = null;
        if (chatImageInput) chatImageInput.value = '';
        if (previewImage) previewImage.src = '';
        if (previewContainer) previewContainer.classList.add('d-none');
    }

    // 4. 메시지 및 이미지 폼 전송
    if (chatForm) {
        chatForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            //  웹소켓 연결 확인
            if (!stompClient || !stompClient.connected) {
                alert("채팅 서버 연결 중입니다. 잠시 후 다시 시도해주세요.");
                return;
            }

            const textContent = messageInput ? messageInput.value.trim() : '';

            if (!selectedFile && !textContent) return;

          try {
              // 이미지 전송 로직
              if (selectedFile) {
                  const formData = new FormData();
                  formData.append('image', selectedFile);

                  const result = await Api.formRequest('/api/chat/image', formData);

                  if (result && result.isAvailable) {
                      const isSent = sendChatMessage(result.data, 'IMAGE');
                      if (isSent) clearSelectedImage();
                  } else {
                      alert(result.message || "이미지 업로드에 실패했습니다.");
                      return;
                  }
              }

              // 일반 메시지 전송
              if (textContent) {
                  const isSent = sendChatMessage(textContent, 'TALK');
                  if (isSent && messageInput) {
                      messageInput.value = ''; // 성공했을 때만 입력창 초기화
                  }
              }

              if (messageInput) messageInput.focus();

          } catch (error) {
              console.error("❌ 폼 전송 처리 중 오류:", error);
          }
       });
    }

    // 5. 채팅방 나가기 로직
    const chat_leave_Btn = document.getElementById('chat_leave_Btn');
    if(chat_leave_Btn) {
        chat_leave_Btn.addEventListener('click', async function() {
            if (!confirm("채팅방을 나가시겠습니까?\n나가면 더 이상 이 대화방을 볼 수 없습니다.")) return;
            try {
                const result = await Api.postRequest(`/api/chat-room/${roomId}/leave`, {}, "POST");
                if (result && result.isAvailable) {
                    alert(result.message || "채팅방을 나갔습니다.");
                    window.location.href = "/items/list/1";
                }
            } catch (error) {
                console.error("나가기 실패 :", error);
                alert(error.message || "오류가 발생하였습니다.");
            }
        });
    }

    // 화면에 이미 그려진 기존 메시지들 중 '마지막 메시지'의 날짜를 가져와 lastRenderedDate 초기화
    initLastRenderedDate();
});

/**
 * 웹소켓 연결 및 방 구독(Subscribe) 설정
 */
function initWebSocket(roomId) {
    stompClient = new StompJs.Client({
        webSocketFactory: function () {
            const protocol = window.location.protocol === 'https:' ? 'https' : 'http';
            return new SockJS(`${protocol}://${window.location.host}/ws-stomp`);
        },
        reconnectDelay: 5000,
        debug: function (str) {
            console.log("STOMP Log: " + str);
        }
    });

    // 중요: 서버와의 연결이 완전히 완료된 후 실행되는 콜백 함수
    stompClient.onConnect = function (frame) {
        console.log('✅ 웹소켓 연결 성공!');

        // 연결 완료 후 방 구독(Subscribe)
        stompClient.subscribe(`/sub/chat/room/${roomId}`, function (chatMessage) {
            const msgBody = JSON.parse(chatMessage.body);
            console.log("📩 [수신 메시지]:", msgBody);

            // 1. 읽음(READ) 이벤트 수신 시 화면의 숫자 '1' 모두 제거
            if (msgBody.type === 'READ') {
                console.log("👀 [READ 신호 수신] 숫자 1 지움");
                document.querySelectorAll('.unread-count').forEach(el => el.remove());
                return;
            }

            // 2. 메시지 렌더링
            renderMessage(msgBody);

            // 3. 접속한 유저와 보낸 사람(senderUsername)을 직접 비교
            const isMine = (msgBody.senderUsername === currentLoginUsername);

            // 내가 보낸 게 아니고, NOTICE도 아니라면 -> "나 이 메시지 읽었음" 신호 전파
            if (!isMine && msgBody.type !== 'NOTICE') {
                console.log("🚀 [READ 신호 전송] -> /pub/chat/read");
                stompClient.publish({
                    destination: "/pub/chat/read",
                    body: JSON.stringify({ roomId: roomId })
                });
            }
        });
    };

    stompClient.onStompError = function (frame) {
        console.error('❌ 웹소켓 브로커 에러 발생:', frame);
    };

    stompClient.activate(); // 소켓 가동
}



/**
 * 공통 메시지 발행(Publish) 함수
 */
function sendChatMessage(content, type = 'TALK') {
    const roomId = document.getElementById('roomId').value;

    // ⭐️ 소켓이 연결되어 있는지 엄격하게 확인 (stompClient.connected)
    if (!stompClient || !stompClient.connected) {
        alert("웹소켓 연결 중입니다. 잠시 후 다시 시도해주세요.");
        console.warn("⚠️ STOMP 미연결 상태에서 전송 시도됨");
        return false; // 전송 실패 리턴
    }

    const messagePayload = {
        roomId: roomId,
        content: content,
        type: type
    };

    try {
        stompClient.publish({
            destination: "/pub/chat/message",
            body: JSON.stringify(messagePayload)
        });
        return true;
    } catch (error) {
        console.error("❌ 메시지 publish 에러:", error);
        return false;
    }
}

/**
 * 다양한 형태의 createdAt 데이터를 안전하게 dayjs 객체로 변환
 */
function parseToDayjs(createdAt) {
    if (!createdAt) return dayjs();

    if (Array.isArray(createdAt)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = createdAt;
        return dayjs(new Date(year, month - 1, day, hour, minute, second));
    }

    return dayjs(createdAt);
}

/**
 * 초기 로딩 시 마지막 메시지의 날짜 구하기
 */
function initLastRenderedDate() {
    const messageArea = document.getElementById('message-area');
    if (!messageArea) return;

    const separators = messageArea.querySelectorAll('.date-separator');

    if (separators.length > 0) {
        const lastDateSeparator = separators[separators.length - 1];
        lastRenderedDate = lastDateSeparator.dataset.date ? lastDateSeparator.dataset.date.trim() : null;
    } else {
        lastRenderedDate = null;
    }
}

/**
 * 실시간 메시지 렌더링
 */
function renderMessage(msg) {
    const messageArea = document.getElementById('message-area');
    if (!messageArea) return;

    if (msg.type === 'NOTICE') {
        const noticeHtml = `
            <div class="d-flex justify-content-center my-3 w-100">
                <div class="bg-light text-muted small px-3 py-1 rounded-pill shadow-sm border">
                    <i class="fa-solid fa-info-circle me-1"></i> ${msg.content}
                </div>
            </div>
        `;
        messageArea.insertAdjacentHTML('beforeend', noticeHtml);
        messageArea.scrollTop = messageArea.scrollHeight;
        return;
    }

    const msgDateObj = parseToDayjs(msg.createdAt);
    const currentDate = msgDateObj.format('YYYY-MM-DD');

    if (lastRenderedDate !== currentDate) {
        renderDateSeparator(msgDateObj);
        lastRenderedDate = currentDate;
    }

    // [수정 1] 백엔드의 msg.isMine을 완전 무시하고, 아이디(Username) 비교만 수행
    const isMine = String(msg.senderUsername) === String(currentLoginUsername);

    // [수정 2] senderNickname이 null이면 senderUsername으로 대체
    const displayNickname = msg.senderNickname || msg.senderUsername || '상대방';

    let contentHtml = '';
    if (msg.type === 'IMAGE') {

        contentHtml = `<img src="${msg.content}"
         class="chat-img-content" style="max-width: 200px; border-radius: 10px; cursor: pointer;" alt="채팅 이미지">`;
    } else {
        contentHtml = `<div class="message-content">${msg.content}</div>`;
    }

    const timeText = msgDateObj.format('A h:mm');

    const html = `
        <div class="d-flex mb-3 ${isMine ? 'justify-content-end' : 'justify-content-start'} w-100">
            <div class="chat-bubble shadow-sm ${isMine ? 'mine-bubble' : 'partner-bubble'}">
                ${!isMine ? `<p class="mb-1 small fw-bold text-primary">${displayNickname}</p>` : ''}
                ${contentHtml}
                <div class="d-flex align-items-center ${isMine ? 'justify-content-end' : 'justify-content-start'} gap-1 mt-1">
                    ${isMine && !msg.isRead ? '<small class="unread-count text-warning fw-bold" style="font-size: 0.7rem;">1</small>' : ''}
                    <small class="chat-time text-muted">${timeText}</small>
                </div>
            </div>
        </div>
    `;

    messageArea.insertAdjacentHTML('beforeend', html);
    messageArea.scrollTop = messageArea.scrollHeight;
}

/**
 * 날짜 구분선 렌더링
 */
function renderDateSeparator(dayjsObj) {
    if (!dayjsObj || !dayjsObj.isValid()) return;

    const rawDate = dayjsObj.format('YYYY-MM-DD');
    const formattedDate = dayjsObj.format('YYYY년 M월 D일 dddd');

    const html = `
        <div class="d-flex justify-content-center my-3 w-100 date-separator" data-date="${rawDate}">
            <div class="bg-light text-muted small px-3 py-1 rounded-pill shadow-sm border">
                <i class="fa-regular fa-calendar me-1"></i> ${formattedDate}
            </div>
        </div>
    `;

    const messageArea = document.getElementById('message-area');
    if (messageArea) {
        messageArea.insertAdjacentHTML('beforeend', html);
    }
}