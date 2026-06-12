package com.ssemi.sampleorder.repository.json;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.SampleRepository;
import com.ssemi.sampleorder.repository.util.JsonFileUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonSampleRepository implements SampleRepository {

    private final String filePath;

    public JsonSampleRepository(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Sample sample) {
        if (sample == null) throw new IllegalArgumentException("sample은 null일 수 없습니다.");
        JSONArray array = JsonFileUtil.readArray(filePath);
        JSONObject json = toJson(sample);

        for (int i = 0; i < array.length(); i++) {
            if (array.getJSONObject(i).getString("id").equals(sample.getId())) {
                array.put(i, json);
                JsonFileUtil.writeArray(filePath, array);
                return;
            }
        }
        array.put(json);
        JsonFileUtil.writeArray(filePath, array);
    }

    @Override
    public Optional<Sample> findById(String id) {
        if (id == null) throw new IllegalArgumentException("id는 null일 수 없습니다.");
        return JsonFileUtil.readArray(filePath).toList().stream()
                .map(o -> new JSONObject((java.util.Map<?, ?>) o))
                .filter(j -> j.getString("id").equals(id))
                .map(this::fromJson)
                .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        JSONArray array = JsonFileUtil.readArray(filePath);
        List<Sample> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(fromJson(array.getJSONObject(i)));
        }
        return result;
    }

    @Override
    public List<Sample> findByName(String keyword) {
        if (keyword == null) throw new IllegalArgumentException("keyword는 null일 수 없습니다.");
        String lower = keyword.toLowerCase();
        List<Sample> result = new ArrayList<>();
        JSONArray array = JsonFileUtil.readArray(filePath);
        for (int i = 0; i < array.length(); i++) {
            Sample s = fromJson(array.getJSONObject(i));
            if (s.getName().toLowerCase().contains(lower)) result.add(s);
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

    private JSONObject toJson(Sample s) {
        return new JSONObject()
                .put("id", s.getId())
                .put("name", s.getName())
                .put("avgProductionTime", s.getAvgProductionTime())
                .put("yield", s.getYield())
                .put("stock", s.getStock());
    }

    private Sample fromJson(JSONObject j) {
        return new Sample(
                j.getString("id"),
                j.getString("name"),
                j.getInt("avgProductionTime"),
                j.getDouble("yield"),
                j.getInt("stock")
        );
    }
}
