document.getElementById('reviewForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const itemId = document.getElementById('itemId').value;
    const rating = document.querySelector('input[name="rating"]:checked').value;
    const content = document.getElementById('content').value;

    fetch(`/reviews/item/${itemId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating: rating, content: content })
    })
    .then(res => res.json())
    .then(data => {
        if(data.success) {
            alert("소중한 후기가 성공적으로 배달되었습니다!");
            location.href = '/users/mypage';
        } else { alert(data.message); }
    });
});