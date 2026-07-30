package com.example.demo.Repository.Item.Item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;


public interface ItemRepositoryCustom {

    Page<Item> findAllByKeyWord(String kw, Long categoryId,
                                Pageable pageable, String kw_type);

    void updateView(Long id);

    // 시스템 정책용 벌크 Hard Delete (기존 동일)
    void deleteAllByIdsIn(List<Long> itemIds);

    //  관리자용: 소프트 삭제된 게시글 중 [제목 OR 내용 OR 작성자 닉네임] 통합 검색
    Page<Item> findSoftDeletedItemsByKeyword(String keyword, Pageable pageable);

    //  관리자용: 검색어 없을 때 소프트 삭제된 전체 리스트 페이징 조회
    Page<Item> findSoftDeletedItemsAll(Pageable pageable);

    //  관리자용: 소프트 삭제된 특정 게시글 하나를 연관 데이터와 함께 완벽히 조회
    Optional<Item> findSoftDeletedItemDetailById(Long id);
}
