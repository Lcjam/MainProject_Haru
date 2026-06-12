package com.example.demo.service;

import com.example.demo.dto.hobby.HobbyRequest;
import com.example.demo.mapper.HobbyMapper;
import com.example.demo.model.Category;
import com.example.demo.model.Hobby;
import com.example.demo.model.UserHobby;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HobbyService {

    @Getter // AuthService에서 접근할 수 있도록 getter 추가
    private final HobbyMapper hobbyMapper;

    /**
     * 모든 취미 목록을 조회합니다.
     */
    public List<Hobby> getAllHobbies() {
        return hobbyMapper.getAllHobbies();
    }
    /**
     * 모든 취미와 그에 해당하는 카테고리 목록을 조회합니다.
     */
    public List<Hobby> getAllHobbiesWithCategories() {
        List<Hobby> hobbies = hobbyMapper.getAllHobbies();
        
        // 각 취미에 해당하는 카테고리 목록을 설정
        for (Hobby hobby : hobbies) {
            List<Category> categories = hobbyMapper.getCategoriesByHobbyId(hobby.getHobbyId());
            hobby.setCategories(categories);
        }
        
        return hobbies;
    }
    /**
     * 취미 ID로 취미를 조회합니다.
     */
    public Hobby getHobbyById(Long hobbyId) {
        return hobbyMapper.getHobbyById(hobbyId);
    }

    /**
     * 카테고리 ID로 취미 목록을 조회합니다.
     */
    public List<Hobby> getHobbiesByCategoryId(Long categoryId) {
        return hobbyMapper.getHobbiesByCategoryId(categoryId);
    }

    /**
     * 모든 카테고리 목록을 조회합니다.
     */
    public List<Category> getAllCategories() {
        return hobbyMapper.getAllCategories();
    }

    /**
     * 사용자의 취미 목록을 조회합니다.
     */
    public List<UserHobby> getUserHobbies(String email) {
        return hobbyMapper.getUserHobbies(email);
    }

    /**
     * 사용자의 취미를 등록합니다.
     * @throws IllegalArgumentException 선택한 취미가 해당 카테고리에 속하지 않는 경우
     */
    @Transactional
    public void registerUserHobby(String email, Long hobbyId, Long categoryId) {
        // 취미가 해당 카테고리에 속하는지 확인
        boolean isValid = hobbyMapper.isHobbyInCategory(hobbyId, categoryId);
        if (!isValid) {
            throw new IllegalArgumentException(
                "선택한 취미(ID: " + hobbyId + ")가 해당 카테고리(ID: " + categoryId + ")에 속하지 않습니다."
            );
        }
        
        UserHobby userHobby = UserHobby.builder()
                .email(email)
                .hobbyId(hobbyId)
                .categoryId(categoryId)
                .build();
        
        hobbyMapper.insertUserHobby(userHobby);
       
    }

    /**
     * 사용자의 여러 취미를 등록합니다.
     * 카테고리 -> 취미 선택 방식에 맞게 검증 로직 강화
     */
    @Transactional
    public void registerUserHobbies(String email, List<HobbyRequest> hobbies) {
        if (hobbies == null || hobbies.isEmpty()) {
    
            return;
        }

        // 기존 취미 모두 삭제
        hobbyMapper.deleteAllUserHobbies(email);
        
        // 새로운 취미 등록
        for (HobbyRequest hobby : hobbies) {
            try {
                // categoryId를 기반으로 해당 카테고리에 속하는 취미인지 검증
                if (hobby.getCategoryId() == null) {
                 
                        
                    continue;
                }
                
                registerUserHobby(email, hobby.getHobbyId(), hobby.getCategoryId());
                
            } catch (Exception e) {
           
            }
        }
    }

    /**
     * 취미 ID가 유효한지 확인합니다.
     */
    public boolean isValidHobby(Long hobbyId) {
        return hobbyMapper.getHobbyById(hobbyId) != null;
    }

    /**
     * 카테고리 ID가 유효한지 확인합니다.
     */
    public boolean isValidCategory(Long categoryId) {
        List<Category> categories = hobbyMapper.getAllCategories();
        return categories.stream().anyMatch(c -> c.getCategoryId().equals(categoryId));
    }
}
