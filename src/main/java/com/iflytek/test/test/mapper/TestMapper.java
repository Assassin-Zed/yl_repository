package com.iflytek.test.test.mapper;

import com.iflytek.test.test.model.Test;
import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestMapper {
  void  insert(Test test);
  List<Test> queryAll();
  int updateStatus(List list);
}
