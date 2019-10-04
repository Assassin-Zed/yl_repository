package com.iflytek.test.test;

import com.iflytek.test.test.mapper.TestMapper;
import jdk.nashorn.internal.ir.ReturnNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class TestApplicationTests {
	@Autowired
	public TestMapper testMapper;

	@Test
	public void insertData() {
		for (int i = 1; i < 100000; i++) {
			com.iflytek.test.test.model.Test test = new com.iflytek.test.test.model.Test();
			test.setNo(i);
			test.setStatus(0);
			testMapper.insert(test);
		}
	}

	//不使用线程池
	@Test
	public void update1(){
		List<com.iflytek.test.test.model.Test> tests = testMapper.queryAll();
		List<com.iflytek.test.test.model.Test> tests2 = tests.stream().map((test) -> {
			test.setStatus(1);
			return test;
		}).collect(Collectors.toList());
		//单线程一次执行200条、
		int count =100000/200;
		for (int i = 1; i <= count; i++) {
			List<com.iflytek.test.test.model.Test> tests1 =
					tests2.subList((i - 1) * 200, i * 200);
			testMapper.updateStatus(tests1);
		}
		}

		@Test
		//使用普通线程池和countDownLatch
		public void update2() throws InterruptedException {
			CountDownLatch countDownLatch = new CountDownLatch(10);
			List<com.iflytek.test.test.model.Test> tests = testMapper.queryAll();
			List<com.iflytek.test.test.model.Test> tests2 = tests.stream().map((test) -> {
				test.setStatus(1);
				return test;
			}).collect(Collectors.toList());
		ThreadPoolExecutor executor =new
				ThreadPoolExecutor(10,10,
				1l,TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(10));
			for (int i = 1; i <= 10; i++) {
				List<com.iflytek.test.test.model.Test> tests1 =
						tests2.subList((i - 1) * 10000, i * 10000);
				executor.execute(()->{
					//单线程一次执行200条、
					int count =10000/200;
					for (int j = 1; j <= count; j++) {
						List<com.iflytek.test.test.model.Test> tests3= tests1.subList((j - 1) * 200, j * 200);
						testMapper.updateStatus(tests3);
					}
					countDownLatch.countDown();
				});
			}
			countDownLatch.await();
       }

       //使用forkAndJoin框架
      @Test
	   public void update3() throws InterruptedException, ExecutionException {
		  List<com.iflytek.test.test.model.Test> tests = testMapper.queryAll();
		  List<com.iflytek.test.test.model.Test> tests2 = tests.stream().map((test) -> {
			  test.setStatus(1);
			  return test;
		  }).collect(Collectors.toList());
		  ForkJoinPool forkJoinPool = new ForkJoinPool();
		  ForkJoinTask<Integer> submit = forkJoinPool.submit(new UpdateTask(tests2));
		  System.out.println(submit.get());
	  }

	  public class UpdateTask extends RecursiveTask<Integer>{
		private static final int THRESHOLD = 5000;  // 阈值
		public List<com.iflytek.test.test.model.Test> list;
        public UpdateTask(List<com.iflytek.test.test.model.Test> list){
        	this.list=list;
		}
		  @Override
		  protected Integer compute() {
        	int sum=0;
             boolean canCompute= list.size()<=1000;
             if(canCompute){
                 //单线程一次执行200条、
				 int count =list.size()/200+1;
				 for (int j = 1; j <= count; j++) {
					 List<com.iflytek.test.test.model.Test> tests3=
							 list.subList((j - 1) * 200, j * 200>=list.size()?list.size():j*200);
					 int i = testMapper.updateStatus(tests3);
					sum+=i;
				 }
			 }else {
				 // 如果任务大于阈值，就分裂成两个子任务计算
				 int middle = list.size()/2;
				 List<com.iflytek.test.test.model.Test> leftList =
						 list.subList(0, middle);
				 List<com.iflytek.test.test.model.Test> rightList =
						 list.subList(middle, list.size());
				 UpdateTask leftTask = new UpdateTask(leftList);
				 UpdateTask rightTask = new UpdateTask(rightList);
                // 执行子任务
				 leftTask.fork();
				 rightTask.fork();
				 Integer t1 = leftTask.join();
				 Integer t2 = rightTask.join();
				 sum=t1+t2;
			 }
			  return sum;
		  }
	  }
}
