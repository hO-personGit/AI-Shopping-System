package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.ai.AiCopywritingRequest;
import org.example.springboot.dto.ai.AiCopywritingResponse;
import org.example.springboot.dto.ai.AiProductRecommendation;
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

    @org.example.springboot.ratelimit.CircuitBreaker(name = "ai-service", failureThreshold = 3, windowSeconds = 10, openSeconds = 15)
    @org.example.springboot.ratelimit.RateLimit(key = "ai-guide", permitsPerSecond = 50, capacity = 100)
    public AiGuideResponse smartGuide(AiGuideRequest request) {
        if (request.getTopK() == null) {
            request.setTopK(5);
        }
        return post("/ai/guide", request, AiGuideResponse.class);
    }

    @org.example.springboot.ratelimit.CircuitBreaker(name = "ai-service", failureThreshold = 3, windowSeconds = 10, openSeconds = 15)
    @org.example.springboot.ratelimit.RateLimit(key = "ai-copywriting", permitsPerSecond = 30, capacity = 60)
    public AiCopywritingResponse generateCopywriting(AiCopywritingRequest request) {
        return post("/ai/copywriting", request, AiCopywritingResponse.class);
    }

    @org.example.springboot.ratelimit.CircuitBreaker(name = "ai-service", failureThreshold = 3, windowSeconds = 10, openSeconds = 15)
    @org.example.springboot.ratelimit.RateLimit(key = "ai-sales-analysis", permitsPerSecond = 20, capacity = 40)
    public AiSalesAnalysisResponse analyzeSales(AiSalesAnalysisRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("salesData", buildSalesAnalysisData(request));
        return post("/ai/sales-analysis", payload, AiSalesAnalysisResponse.class);
    }

    // ================= 降级兜底（熔断/AI 不可用时本地返回） =================

    /** 降级：AI 导购不可用时，按销量热榜返回本地推荐。 */
    public AiGuideResponse fallbackGuide(AiGuideRequest request) {
        int topK = request == null || request.getTopK() == null ? 5 : request.getTopK();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSalesCount)
                .last("LIMIT " + Math.min(Math.max(topK, 1), 20)));
        List<AiProductRecommendation> recs = products.stream().map(p -> {
            AiProductRecommendation r = new AiProductRecommendation();
            r.setId(p.getId());
            r.setName(p.getName());
            r.setCategory(p.getCategoryId() == null ? null : String.valueOf(p.getCategoryId()));
            r.setPrice(p.getPrice() == null ? null : p.getPrice().doubleValue());
            r.setStock(p.getStock());
            r.setSalesCount(p.getSalesCount());
            r.setReason("AI 服务暂不可用，按销量热榜降级推荐");
            r.setScore(0.0);
            return r;
        }).collect(java.util.stream.Collectors.toList());
        AiGuideResponse resp = new AiGuideResponse();
        resp.setAnswer("AI 智能导购服务暂不可用，以下为按销量热榜的降级推荐（自动兜底）。");
        resp.setRecommendations(recs);
        resp.setSource("fallback");
        return resp;
    }

    /** 降级：文案生成不可用时返回模板文案。 */
    public AiCopywritingResponse fallbackCopywriting(AiCopywritingRequest request) {
        String name = request == null || request.getName() == null ? "本商品" : request.getName();
        AiCopywritingResponse resp = new AiCopywritingResponse();
        resp.setTitle(name + " 限时特惠");
        resp.setSummary("品质好物，限时特惠，性价比超高！");
        resp.setDetail("精选优质好物，下单立享优惠，错过再等一年。");
        resp.setSlogan("好物不贵，值得拥有！");
        resp.setSource("fallback");
        return resp;
    }

    /** 降级：销售分析不可用时返回本地统计兜底。 */
    public AiSalesAnalysisResponse fallbackSalesAnalysis() {
        AiSalesAnalysisResponse resp = new AiSalesAnalysisResponse();
        resp.setHotProductsAnalysis("AI 服务暂不可用，以下为本地统计兜底。");
        resp.setStockWarning("库存低于 20 的商品共 " + getLowStockProducts(null).size() + " 款，建议及时补货。");
        resp.setReplenishmentAdvice("建议优先补充销量 Top 商品的库存。");
        resp.setSalesTrendSummary("本地订单/销量统计请参考销售分析模块。");
        resp.setSummary("服务降级中，数据为本地统计结果。");
        resp.setSource("fallback");
        return resp;
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