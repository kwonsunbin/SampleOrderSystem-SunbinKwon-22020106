package com.ssemi.sampleorder.repository.json;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.util.JsonFileUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonOrderRepository implements OrderRepository {

    private final String filePath;

    public JsonOrderRepository(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Order order) {
        if (order == null) throw new IllegalArgumentException("order는 null일 수 없습니다.");
        JSONArray array = JsonFileUtil.readArray(filePath);
        JSONObject json = toJson(order);

        for (int i = 0; i < array.length(); i++) {
            if (array.getJSONObject(i).getString("id").equals(order.getId())) {
                array.put(i, json);
                JsonFileUtil.writeArray(filePath, array);
                return;
            }
        }
        array.put(json);
        JsonFileUtil.writeArray(filePath, array);
    }

    @Override
    public Optional<Order> findById(String id) {
        if (id == null) throw new IllegalArgumentException("id는 null일 수 없습니다.");
        JSONArray array = JsonFileUtil.readArray(filePath);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (obj.getString("id").equals(id)) return Optional.of(fromJson(obj));
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findAll() {
        JSONArray array = JsonFileUtil.readArray(filePath);
        List<Order> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(fromJson(array.getJSONObject(i)));
        }
        return result;
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("status는 null일 수 없습니다.");
        List<Order> result = new ArrayList<>();
        JSONArray array = JsonFileUtil.readArray(filePath);
        for (int i = 0; i < array.length(); i++) {
            Order order = fromJson(array.getJSONObject(i));
            if (order.getStatus() == status) result.add(order);
        }
        return result;
    }

    @Override
    public void delete(String id) {
        if (id == null) throw new IllegalArgumentException("id는 null일 수 없습니다.");
        JSONArray array = JsonFileUtil.readArray(filePath);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (!obj.getString("id").equals(id)) updated.put(obj);
        }
        JsonFileUtil.writeArray(filePath, updated);
    }

    @Override
    public void deleteAll() {
        JsonFileUtil.writeArray(filePath, new JSONArray());
    }

    private JSONObject toJson(Order o) {
        JSONObject obj = new JSONObject()
                .put("id", o.getId())
                .put("sampleId", o.getSampleId())
                .put("customerName", o.getCustomerName())
                .put("quantity", o.getQuantity())
                .put("status", o.getStatus().name())
                .put("createdAt", o.getCreatedAt().withNano(0).toString())
                .put("totalProductionSeconds", o.getTotalProductionSeconds());
        if (o.getProductionStartedAt() != null)
            obj.put("productionStartedAt", o.getProductionStartedAt().withNano(0).toString());
        return obj;
    }

    private Order fromJson(JSONObject j) {
        LocalDateTime productionStartedAt = j.has("productionStartedAt") && !j.isNull("productionStartedAt")
                ? LocalDateTime.parse(j.getString("productionStartedAt"))
                : null;
        // 구 포맷(totalProductionMinutes) 하위 호환 처리
        int totalProductionSeconds = j.has("totalProductionSeconds")
                ? j.getInt("totalProductionSeconds")
                : j.optInt("totalProductionMinutes", 0);
        return new Order(
                j.getString("id"),
                j.getString("sampleId"),
                j.getString("customerName"),
                j.getInt("quantity"),
                OrderStatus.valueOf(j.getString("status")),
                LocalDateTime.parse(j.getString("createdAt")),
                productionStartedAt,
                totalProductionSeconds
        );
    }
}
