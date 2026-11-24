package com.example.demo.repository;

import com.example.demo.repository.entity.Yuna;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DemoRepository {

    int insertYuna(Yuna yuna);

    List<Yuna> findAll();

    boolean existsByYuna(int yuna);

    int updateYuna(Yuna yuna);

    Yuna getByYuna(int yuna);

    void deleteByYuna(int yuna);
}
