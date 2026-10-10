import { useState, useEffect, useRef } from 'react';

export default function App() {

    const [roomData, setRoomData] = useState(null);
    const [messages, setMessages] = useState([]);
    const [inputText, setInputText] = useState('');

    const messageAreaRef = useRef(null);

   // 1. [데이터 로딩] 백엔드 REST API에서 채팅방 데이터 가져오기 (새로 추가!)
   useEffect(() => {
     // HTML 태그에 심겨진 roomId 가져오기
     const rootEl = document.getElementById('react-chat-root');
     const roomId = rootEl?.dataset.roomId;

     if (roomId) {
       // 스프링 부트 REST API 호출
       fetch(`/api/chat-room/${roomId}`)
         .then((res) => res.json())
         .then((data) => {
           setRoomData(data); // 방 정보 저장 (itemTitle, partnerNickname 등)
           setMessages(data.messages || []); // 기존 메시지 목록 저장
         })
         .catch((err) => console.error("채팅방 데이터 로딩 실패:", err));
     }
   }, []); // [] : 처음에 화면 띄워질 때 1번만 실행

   // 2. [자동 스크롤] 메시지가 추가/변경될 때마다 맨 아래로 이동 (유지!)
   useEffect(() => {
     if (messageAreaRef.current) {
       messageAreaRef.current.scrollTop = messageAreaRef.current.scrollHeight;
     }
   }, [messages]);

   // 3. 로딩 상태 처리 (유지!)
   if (!roomData) {
     return <div className="p-4 text-center">채팅방 정보를 불러오는 중입니다...</div>;
   }

  // 4. 실제 JSX 화면 렌더링 
  return (
    <div className="card shadow-sm border-0 mx-auto chat-card" style={{ maxWidth: '600px' }}>

      {/* 1. 헤더 (상품 정보 & 상대방 닉네임) */}
      <div className="card-header bg-white border-bottom py-3 px-4 d-flex align-items-center justify-content-between">
        <div className="d-flex align-items-center gap-3">
          <a href={`/items/detail/${roomData.itemId}`} className="text-decoration-none">
            <img
              src={roomData.itemImage}
              className="rounded border"
              style={{ width: '50px', height: '50px', objectFit: 'cover' }}
              alt="상품이미지"
            />
          </a>
          <div>
            <h6 className="fw-bold mb-0 text-dark">{roomData.itemTitle}</h6>
            <small className="text-muted">
              <span>{roomData.partnerNickname}</span>님과의 대화
            </small>
          </div>
        </div>
        <button className="btn btn-outline-danger btn-sm border-0">
          <i className="fa-solid fa-right-from-bracket me-1"></i> 나가기
        </button>
      </div>

      {/* 2. 메시지 영역 */}
      <div
        className="card-body p-4"
        ref={messageAreaRef}
        style={{ height: '400px', overflowY: 'auto' }}
      >
        <div className="d-flex justify-content-center my-3">
          <span className="bg-light text-muted small px-3 py-1 rounded-pill border shadow-sm">
            리액트 채팅 UI 연동 완료! 🎉
          </span>
        </div>

        {/* 메시지 목록 반복 출력 (추후 소켓 연결) */}
        {messages.map((msg, index) => (
          <div
            key={index}
            className={`d-flex mb-3 ${msg.isMine ? 'justify-content-end' : 'justify-content-start'} w-100`}
          >
            <div className={`chat-bubble shadow-sm ${msg.isMine ? 'mine-bubble' : 'partner-bubble'}`}>
              {!msg.isMine && <p className="mb-1 small fw-bold text-primary">{msg.senderNickname}</p>}
              <div className="message-content">{msg.content}</div>
              <small className="chat-time text-muted">{msg.timeText}</small>
            </div>
          </div>
        ))}
      </div>

      {/* 3. 메시지 입력 폼 */}
      <div className="card-footer bg-white border-top p-3">
        <form className="d-flex gap-2 align-items-center" onSubmit={(e) => e.preventDefault()}>
          <button type="button" className="btn btn-outline-secondary" title="사진 첨부">
            <i className="fa-regular fa-image fs-5"></i>
          </button>

          <input
            type="text"
            className="form-control border-0 bg-light"
            placeholder="메시지를 입력하세요..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
          />

          <button type="submit" className="btn btn-primary px-4">
            전송
          </button>
        </form>
      </div>

    </div>
  );
}