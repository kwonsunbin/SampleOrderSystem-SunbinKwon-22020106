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
import com.ssemi.sampleorder.service.DataInitializer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    private static final String SAMPLES_PATH = "data/samples.json";
    private static final String ORDERS_PATH  = "data/orders.json";

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--clean")) {
            Files.deleteIfExists(Paths.get(SAMPLES_PATH));
            Files.deleteIfExists(Paths.get(ORDERS_PATH));
            System.out.println("[clean] 기존 데이터를 초기화하고 새로 시작합니다.");
        }

        JsonSampleRepository sampleRepo = new JsonSampleRepository(SAMPLES_PATH);
        JsonOrderRepository orderRepo   = new JsonOrderRepository(ORDERS_PATH);
        ProductionQueue queue           = new ProductionQueue();

        DataInitializer dataInitializer = new DataInitializer(sampleRepo, orderRepo);
        dataInitializer.seedSamples();
        dataInitializer.clearOrders();

        SampleService     sampleService     = new SampleService(sampleRepo);
        OrderService      orderService      = new OrderService(orderRepo, sampleRepo, queue);
        ProductionService productionService = new ProductionService(orderRepo, sampleRepo, queue);
        MonitorService    monitorService    = new MonitorService(orderRepo, sampleRepo);

        ConsoleView consoleView = new ConsoleView(System.in, System.out);

        SampleController     sampleController     = new SampleController(sampleService, consoleView);
        OrderController      orderController      = new OrderController(orderService, sampleService,
                                                        productionService, consoleView);
        ProductionController productionController = new ProductionController(productionService,
                                                        sampleService, consoleView);
        MonitorController    monitorController    = new MonitorController(monitorService, consoleView);
        ReleaseController    releaseController    = new ReleaseController(orderService, consoleView);

        MainController mainController = new MainController(
            sampleController, orderController, productionController,
            monitorController, releaseController, consoleView,
            sampleService, monitorService
        );
        mainController.run();
    }
}
