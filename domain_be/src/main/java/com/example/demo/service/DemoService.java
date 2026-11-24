//실질적으로 데이터가 어떻게 처리가 되어가고 있는지 규칙을 정의해둔 곳

package com.example.demo.service;

import com.example.demo.controller.dto.YunaCreateRequest;
import com.example.demo.controller.dto.YunaGetResponse;
import com.example.demo.controller.dto.YunaUpdateRequest;
import com.example.demo.repository.DemoRepository;
import com.example.demo.repository.entity.Yuna;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)//트랜잭션을 읽기 전용(readOnly = true) 모드로 설정합니다.
public class DemoService {

    private final DemoRepository demoRepository;

    @Transactional
    public int create(YunaCreateRequest request) {
        Yuna yuna = Yuna.createYuna(request.getYuna(), request.getAge(), request.getHobby(), request.getJob());
        return demoRepository.
                insertYuna(yuna);

    }

    public List<YunaGetResponse> getAll() {
        return demoRepository.findAll().stream()
                .map(YunaGetResponse::responseYuna)
                .toList();
    }


    public YunaGetResponse getByYuna(int yuna) {
        Yuna yuna1 = demoRepository.getByYuna(yuna);
        if (yuna1 == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유나 정보가 없습니다.");
        }
        return YunaGetResponse.responseYuna(yuna1);

    }

    @Transactional
    public void updateYuna(int yuna, YunaUpdateRequest request) {
        Yuna yuna2 = demoRepository.getByYuna(yuna);
        if (yuna2 != null) {
            yuna2.setAge(request.getAge());
            yuna2.setJob(request.getJob());
            yuna2.setHobby(request.getHobby());


            int count = demoRepository.updateYuna(yuna2);
        }
    }

    @Transactional
    public void deleteYuna(int yuna) {
        boolean existsByYuna = demoRepository.existsByYuna(yuna);
        if (existsByYuna) {
            demoRepository.deleteByYuna(yuna);
        }

    }
}
