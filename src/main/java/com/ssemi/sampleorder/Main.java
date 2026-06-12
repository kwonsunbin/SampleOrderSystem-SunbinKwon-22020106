package com.ssemi.sampleorder;

import com.ssemi.sampleorder.repository.json.JsonSampleRepository;
import com.ssemi.sampleorder.repository.json.JsonOrderRepository;
import com.ssemi.sampleorder.model.ProductionQueue;
import com.ssemi.sampleorder.service.SampleService;
import com.ssemi.sampleorder.service.OrderService;
import com.ssemi.sampleorder.service.ProductionService;
import com.ssemi.sampleorder.service.MonitorService;
import com.ssemi.sampleorder.view.ConsoleView;
import com.ssemi.sampleorder.controller.SampleController;
import com.ssemi.sampleorder.controller.OrderController;
import com.ssemi.sampleorder.controller.ProductionController;
import com.ssemi.sampleorder.controller.MonitorController;
import com.ssemi.sampleorder.controller.ReleaseController;
import com.ssemi.sampleorder.controller.MainController;

public class Main {
    public static void main(String[] args) {
        JsonSampleRepository sampleRepo = new JsonSampleRepository("data/samples.json");
        JsonOrderRepository orderRepo = new JsonOrderRepository("data/orders.json");
        ProductionQueue queue = new ProductionQueue();
        SampleService sampleService = new SampleService(sampleRepo);
        OrderService orderService = new OrderService(orderRepo, sampleRepo, queue);
        ProductionService productionService = new ProductionService(orderRepo, sampleRepo, queue);
        MonitorService monitorService = new MonitorService(orderRepo, sampleRepo);
        ConsoleView consoleView = new ConsoleView(System.in, System.out);
        SampleController sampleController = new SampleController(sampleService, consoleView);
        OrderController orderController = new OrderController(orderService, consoleView);
        ProductionController productionController = new ProductionController(productionService, consoleView);
        MonitorController monitorController = new MonitorController(monitorService, consoleView);
        ReleaseController releaseController = new ReleaseController(orderService, consoleView);
        MainController mainController = new MainController(sampleController, orderController,
                productionController, monitorController, releaseController, consoleView);
        mainController.run();
    }
}
