package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 数据库原子扣减库存与累加销量（防超卖兜底）。
     *
     * <p>通过 WHERE stock &gt;= quantity 保证「扣减+校验」原子完成，
     * 返回受影响行数：0 表示库存不足未扣减，1 表示扣减成功。
     *
     * @param productId 商品 ID
     * @param quantity  购买数量
     * @return 受影响行数
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, sales_count = sales_count + #{quantity} "
            + "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
