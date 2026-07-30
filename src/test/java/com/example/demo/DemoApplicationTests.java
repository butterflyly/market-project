package com.example.demo;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Item.ItemCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private ItemCategoryService itemCategoryService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@Rollback(false)
	void 아이템_생성() {

		List<ItemCategory> itemCategoryList = itemCategoryService.findAll();

		// 1. 리스트 크기 확인
		int listSize = itemCategoryList.size();

		// 2. 랜덤 인덱스 생성 (0부터 listSize - 1 사이의 값)
		Random random = new Random();

		for(int i=58;i<500;i++)
		{
			// 3. 랜덤하게 하나 꺼내기
			ItemCategory randomCategory = itemCategoryList.get(random.nextInt(itemCategoryList.size()));


			// 유저 랜덤 추출 (test0 ~ test29 중 하나)
			// 만약 DB에서 직접 유저를 조회해서 넣고 싶다면 아래처럼 하세요.
			String randomUsername = "test" + random.nextInt(30);
			Users randomUser = userRepository.findByUsernameAndDeletedFalse(randomUsername)
					.orElseThrow(); // 유저가 반드시 있다는 가정 하에

			Item item =  Item.builder().title("테스트 게시글" + i)
					.content("테스트 내용" +i)
					.seller(randomUser)
					.itemCategory(randomCategory)
					.createdAt(LocalDateTime.now())
					.build();

			itemRepository.save(item);
		}

	}

	@Test
	@Rollback(false)
	void 유저_생성()
	{
		for(int i=0; i<30; i++)
		{
			Users user = Users.builder()
					.username("test" +i)
					.password(passwordEncoder.encode("1q2w3e4r5t!@"))
					.nickname("테스트_닉네임"+i)
					.displayNickname("테스트_닉네임"+i)
					.email("임시이메일@naver.com"+i)
					.role(UserRole.NORMAL)
					.createdAt(LocalDateTime.now())
					.build();

			userRepository.save(user);
		}
	}
}
