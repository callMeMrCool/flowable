package com.alan.flowable;

import com.alan.config.MyProcessEngineConfiguration;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author acer
 */
@SpringBootApplication
@RestController
@Import(MyProcessEngineConfiguration.class)
public class FlowableApplication {

    @Autowired
    ProcessEngine processEngineBean;  //流程引擎

    public static void main(String[] args) {
        SpringApplication.run(FlowableApplication.class, args);
    }

    //请假申请
    @RequestMapping(value = "/holidayRequest")
    public String holidayRequest(String employee, Integer nrOfHolidays, String description){
        //将流程部署到引擎中
        RepositoryService repositoryService = processEngineBean.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        //通过API查询验证流程定义已经部署在引擎中
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        System.out.println("Found process definition :" + processDefinition.getName());

        //启动一个流程实例
        RuntimeService runtimeService = processEngineBean.getRuntimeService();
        Map<String, Object> runMap = new HashMap<String, Object>();
        runMap.put("employee", employee);
        runMap.put("nrOfHolidays", nrOfHolidays);
        runMap.put("description", description);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", runMap);
        System.out.println("流程实例id：" + processInstance.getId());

        return "SUCCESS";
    }

    //获取待处理请假申请tasks并处理
    @RequestMapping("getTasks")
    public String getTasks(){
        //获得实际的任务列表
        TaskService taskService = processEngineBean.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        Scanner scanner= new Scanner(System.in);
        //使用任务Id获取特定流程实例的变量
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex-1);

        Map<String,Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " + processVariables.get("nrOfHolidays")
                + " of holidays. Do you approve this?");

        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        Map<String,Object> variables = new HashMap<String, Object>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        return "SUCCESS";
    }

    //获取历史请假处理,实例id为空时查询全部
    @RequestMapping("getHistoryService")
    public String getHistoryService(String processInstanceId){
        HistoryService historyService = processEngineBean.getHistoryService();

        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc().list();

        for (HistoricActivityInstance activity:activities){
            System.out.println(activity.getProcessInstanceId()
                    + ":" + activity.getActivityId() + " took "
                + activity.getDurationInMillis() + " milliseconds");
        }
        return "SUCCESS";
    }
}
