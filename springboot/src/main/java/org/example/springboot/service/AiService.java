package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.ai.AiCopywritingRequest;
import org.example.springboot.dto.ai.AiCopywritingResponse;
import org.example.springboot.dto.ai.AiGuideRequest;
import org.example.springboot.dto.ai.AiGuideResponse;
import org.example.springboot.dto.ai.AiSalesAnalysisRequest;
import org.example.springboot.dto.ai.AiSalesAnalysisResponse;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Resource
    private RestTemplate aiRestTemplate;

    @Resource
    private StatisticsService statisticsService;

    @Resource
    private ProductMapper productMapper;

    @Value("${ai.service.base-url:http://localhost:8001}")
    private String aiServiceBaseUrl;

    public AiGuideResponse smartGuide(AiGuideRequest request) {
        if (request.getTopK() == null) {
            request.setTopK(5);
        }
        return post("/ai/guide", request, AiGuideResponse.class);
    }

    public AiCopywritingResponse generateCopywriting(AiCopywritingRequest request) {
        return post("/ai/copywriting", request, AiCopywritingResponse.class);
    }

    public AiSalesAnalysisResponse analyzeSales(AiSalesAnalysisRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("salesData", buildSalesAnalysisData(request));
        return post("/ai/sales-analysis", payload, AiSalesAnalysisResponse.class);
    }

    public Map<String, Object> buildSalesAnalysisData(AiSalesAnalysisRequest request) {
        Long merchantId = request == null ? null : request.getMerchantId();
        Map<String, Object> data = new HashMap<>();
        data.put("monthlyOrders", statisticsService.getMonthlyOrderStatistics(merchantId));
        data.put("monthlySales", statisticsService.getMonthlySalesStatistics(merchantId));
        data.put("topProducts", statisticsService.getTopSellingProducts());
        data.put("categorySales", statisticsService.getCategorySalesStatistics());
        data.put("lowStockProducts", getLowStockProducts(merchantId));
        data.put("productTotal", productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)));
        return data;
    }

    private List<Product> getLowStockProducts(Long merchantId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .le(Product::getStock, 20)
                .orderByAsc(Product::getStock)
                .last("LIMIT 10");
        if (merchantId != null) {
            wrapper.eq(Product::getMerchantId, merchantId);
        }
        return productMapper.selectList(wrapper);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            T response = aiRestTemplate.postForObject(aiServiceBaseUrl + path, body, responseType);
            if (response == null) {
                throw new IllegalStateException("AI service returned empty response");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalStateException("AI service call failed. Please start FastAPI service: " + ex.getMessage(), ex);
        }
    }
}