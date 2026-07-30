const reviewId = document.getElementById('reviewId').value;

// [수정 로직]
document.getElementById('editReviewForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const rating = document.querySelector('input[name="rating"]:checked').value;
    const content = document.getElementById('content').value;

    fetch(`/reviews/${reviewId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating: rating, content: content })
    })
    .then(res => res.json())
    .then(data => {
        if(data.success) {
            alert("후기가 수정되었습니다.");
            location.href = '/users/mypage';
        } else { alert(data.message); }
    });
});

// [삭제 로직]
document.getElementById('deleteReviewBtn').addEventListener('click', function() {
    if(!confirm("후기를 삭제하시면 판매자의 매너 지수에도 영향이 갑니다. 정말 삭제하시겠습니까?")) return;

    fetch(`/reviews/${reviewId}`, { method: 'DELETE' })
    .then(res => res.json())
    .then(data => {
        if(data.success) {
            alert("후기가 정상적으로 삭제되었습니다.");
            location.href = '/users/mypage';
        } else { alert(data.message); }
    });
});