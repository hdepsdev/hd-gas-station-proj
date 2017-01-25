import com.bhz.eps.entity.Item;
import com.bhz.eps.entity.Order;
import com.bhz.eps.entity.SaleItemEntity;
import com.bhz.eps.service.ItemService;
import com.bhz.eps.service.OrderService;
import com.bhz.eps.service.SaleItemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by summer on 2017/01/25.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/conf/application-context.xml",
        "classpath:/conf/application-dbcfg.xml"})
public class TestDB {
    @Resource
    ItemService itemService;
    @Resource
    SaleItemService saleItemService;
    @Resource
    OrderService orderService;

    @Test
    public void testAdd() throws Exception {
        Item item = new Item();
        item.setCode("testCode");
        item.setName("testName");
        item.setCatalog(-1);
        itemService.addItem(item);

        Order order = new Order();
        order.setOrderId("testOrder");
        order.setOrderTime(new Date().getTime());
        order.setOriginalAmount(new BigDecimal(0));
        orderService.addOrder(order);

        SaleItemEntity saleItem = new SaleItemEntity();
        saleItem.setId("testId");
        saleItem.setProductCode("testCode");
        saleItem.setOrderId("testOrder");
        saleItem.setItemCode("");
        saleItem.setAmount(new BigDecimal(0));
        saleItem.setQuantity(new BigDecimal(0));
        saleItem.setUnitMeasure("");
        saleItem.setUnitPrice(new BigDecimal(0));
        saleItemService.addSaleItem(saleItem);
    }

    @Test
    public void testGet() throws Exception {
        Item item = itemService.getItembyCode("testCode");
        Order order = orderService.getOrderbyId("testOrder");
        List<SaleItemEntity> list = saleItemService.getSaleItemsbyOrderId("testOrder");
        list.size();
    }
}
