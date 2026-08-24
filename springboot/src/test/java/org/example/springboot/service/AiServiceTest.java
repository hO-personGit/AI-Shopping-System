package org.example.springboot.service;

import org.example.springboot.dto.ai.AiGuideRequest;
import org.example.springboot.dto.ai.AiGuideResponse;
import org.example.springboot.dto.ai.AiSalesAnalysisRequest;
import org.example.springboot.dto.ai.AiSalesAnalysisResponse;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 服务调用单元测试：验证 SpringBoot 侧对 FastAPI 微服务的网关转发与销售数据组装。
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RestTemplate aiRestTemplate;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "aiServiceBaseUrl", "http://localhost:8001");
    }

    @Test
    void smartGuide_shouldForwardToFastApiAndReturnResponse() {
        AiGuideResponse mockResp = new AiGuideResponse();
        mockResp.setAnswer("推荐小米手环");
        mockResp.setSource("mock");

        when(aiRestTemplate.postForObject(
                eq("http://localhost:8001/ai/guide"), any(), eq(AiGuideResponse.class)))
                .thenReturn(mockResp);

        AiGuideRequest request = new AiGuideRequest();
        request.setQuery("推荐手环");
        AiGuideResponse result = aiService.smartGuide(request);

        assertEquals("推荐小米手环", result.getAnswer());
        verify(aiRestTemplate, times(1)).postForObject(
                eq("http://localhost:8001/ai/guide"), any(), eq(AiGuideResponse.class));
    }

    @Test
    void smartGuide_shouldDefaultTopKToFive() {
        when(aiRestTemplate.postForObject(
                eq("http://localhost:8001/ai/guide"), any(), eq(AiGuideResponse.class)))
                .thenReturn(new AiGuideResponse());

        AiGuideRequest request = new AiGuideRequest();
        request.setQuery("测试");
        aiService.smartGuide(request);
        assertEquals(5, request.getTopK());
    }

    @Test
    void buildSalesAnalysisData_shouldAssembleStatsAndLowStock() {
        Product lowStock = new Product();
        lowStock.setId(1L);
        lowStock.setStock(5);

        when(productMapper.selectList(any())).thenReturn(List.of(lowStock));
        when(productMapper.selectCount(any())).thenReturn(10L);
        when(statisticsService.getMonthlyOrderStatistics(any())).thenReturn(new java.util.HashMap<>());
        when(statisticsService.getMonthlySalesStatistics(any())).thenReturn(new java.util.HashMap<>());
        when(statisticsService.getTopSellingProducts()).thenReturn(new java.util.HashMap<>());
        when(statisticsService.getCategorySalesStatistics()).thenReturn(new java.util.HashMap<>());

        var data = aiService.buildSalesAnalysisData(new AiSalesAnalysisRequest());

        assertNotNull(data.get("monthlyOrders"));
        assertNotNull(data.get("topProducts"));
        assertEquals(10L, data.get("productTotal"));
        assertEquals(1, ((List<?>) data.get("lowStockProducts")).size());
    }
}
