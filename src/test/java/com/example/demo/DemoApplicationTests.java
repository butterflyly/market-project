package com.example.demo;

import com.example.demo.DTO.Users.Request.Auth.Register.UserRegisterRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Item.ItemCategoryService;
import com.example.demo.Service.Users.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static java.lang.reflect.Array.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DemoApplicationTests {


}
