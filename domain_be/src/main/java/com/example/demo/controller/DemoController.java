package com.example.demo.controller;

import com.example.demo.controller.dto.*;
import com.example.demo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//클라이언트의 요청을 가장 먼저 받는다
@RestController//이 클래스는 HTTP 요청을 받는 컨트롤러이고, 반환값을 그대로 응답 본문(JSON, 문자열 등)으로 보낸다
@RequestMapping("/demos")//이 클래스의 모든 메서드에 공통으로 붙는 URL 앞부분
@RequiredArgsConstructor


public class DemoController {
    private final DemoService demoService;

    @GetMapping("/start")
    public void method() {
        System.out.println("phase1");
        System.out.println("phase2");
        System.out.println("phase3");
        System.out.println("phase4");
    }

    @PostMapping()
    public YunaCreateResponse method1(@RequestBody YunaCreateRequest request)
    //URL 경로 값을 받는 것이 아니라, 클라이언트가 보내는 JSON 형태의 본문
    {
        int count = demoService.create(request);
        YunaCreateResponse success = YunaCreateResponse.success(count, "유나 객체가 성공적으로 만들어졌습니다.");
        return success;

    }

    @GetMapping()//전체 목록 (프론트 그리드/차트용)
    public List<YunaGetResponse> methodList() {
        return demoService.getAll();
    }

    @GetMapping("/{yuna}")//특정 조건값(예: ID나 이름, 직무 등) 하나만 URL 경로
    public YunaGetResponse method2(@PathVariable int yuna) {
        YunaGetResponse response = demoService.getByYuna(yuna);
        return response;
    }

    @PutMapping("/{yuna}")
    public YunaUpdateResponse method3(
            @PathVariable int yuna, @RequestBody YunaUpdateRequest request) {
        demoService.updateYuna(yuna, request);
        return YunaUpdateResponse.createResponse("업데이트가 성공적으로 되었습니다.");
    }


    @DeleteMapping("/{yuna}")
    public YunaDeleteResponse method4(@PathVariable int yuna) {
        demoService.deleteYuna(yuna);
        return YunaDeleteResponse.createResponse("삭제가 성공적으로 되었습니다.");
    }


}
